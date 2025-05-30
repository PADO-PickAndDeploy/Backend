package com.pado.backend.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pado.backend.domain.Component;
import com.pado.backend.domain.Credential;
import com.pado.backend.domain.Deployment;
import com.pado.backend.domain.Project;
import com.pado.backend.domain.mongo.ComponentSettingDocument;
import com.pado.backend.domain.mongo.ComponentStatusDocument;
import com.pado.backend.dto.response.ChargeEstimateDto;
import com.pado.backend.dto.response.ChargeResultDto;
import com.pado.backend.dto.response.CheckDto;
import com.pado.backend.global.exception.CustomException;
import com.pado.backend.global.exception.InvalidCredentialIdException;
import com.pado.backend.global.exception.ProjectNotFoundException;
import com.pado.backend.global.type.ComponentStatus;
import com.pado.backend.repository.ComponentLinkRepository;
import com.pado.backend.repository.ComponentRepository;
import com.pado.backend.repository.CredentialRepository;
import com.pado.backend.repository.DeploymentRepository;
import com.pado.backend.repository.ProjectRepository;
import com.pado.backend.repository.mongo.ComponentSettingRepository;
import com.pado.backend.repository.mongo.ComponentStatusRepository;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.RequiredArgsConstructor;

// gRPC 관련 import - Spring Boot 3.x 호환 버전
import net.devh.boot.grpc.client.inject.GrpcClient;

import provision.Provisioning.ComponentSpec;
import provision.Provisioning.CostResponse;
import provision.Provisioning.DeploymentRequest;
import provision.Provisioning.EC2Request;
import provision.Provisioning.GitCredential;
import provision.Provisioning.MySQLRequest;
import provision.Provisioning.ReactRequest;
import provision.Provisioning.S3Request;
import provision.Provisioning.SpringRequest;
import provision.ProvisioningServiceGrpc;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ComponentRepository componentRepository;
    private final ProjectRepository projectRepository;
    private final ComponentLinkRepository componentLinkRepository;
    private final CredentialRepository credentialRepository;

    // Mongo
    private final ComponentSettingRepository componentSettingRepository;
    private final ComponentStatusRepository componentStatusRepository;
    private final ComponentStatusStoreService componentStatusStoreService;

    // gRPC
    private final ObjectMapper objectMapper;
    private final Executor deploymentExecutor;

    @GrpcClient("provisioning-service")
    private ProvisioningServiceGrpc.ProvisioningServiceBlockingStub stub;

    public SseEmitter startDeployment(Long projectId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        Project project = projectRepository.findById(projectId)
            .orElseThrow(ProjectNotFoundException::new);

        List<Component> components = componentRepository.findByProject(project);

        // 👉 상태 확인 로그 출력
        components.forEach(component -> {
            String id = component.getComponentId().toString();
            Optional<ComponentStatusDocument> opt = componentStatusRepository.findByComponentId(id);
            System.out.println("Component ID: " + id + ", Status Found: " +
                opt.map(ComponentStatusDocument::getStatus).orElse(null));
        }); // -> 제대로 나옴. 그런데도 실행 안되는 이유는? gRPC 요청 이전 단계에서 이미 실행이 중단되고 있다.
    
        // 기본 검증 로직
        boolean allStart = components.stream().allMatch(component ->
            componentStatusRepository.findByComponentId(component.getComponentId().toString())
                .map(doc -> ComponentStatus.START.equals(doc.getStatus()))
                .orElse(false)
        );

        if (!allStart) {
            sendToEmitter(emitter, "deploy-error", "START 상태가 아닌 컴포넌트가 존재합니다. 설정을 확인해주세요.");
            emitter.complete();
            return emitter;
        }

        // TODO : 배포의 상태도 Enum으로 설정?
        Deployment deployment = Deployment.builder()
            .project(project)
            .status("RUNNING")
            .startTime(LocalDateTime.now())
            .build();
        deploymentRepository.save(deployment);

        final String deploymentId = "deploy-" + projectId + "-" + deployment.getDeploymentId();


        List<ComponentSpec> componentSpecs = new ArrayList<>();

        for (Component component : components) {
            try {
                ComponentSettingDocument settingDoc = componentSettingRepository.findByComponentId(component.getComponentId())
                    .orElseThrow(() -> new CustomException("ComponentId " + component.getComponentId() + " 의 설정 정보가 존재하지 않습니다.", HttpStatus.BAD_REQUEST));

                JsonNode root = objectMapper.readTree(settingDoc.getSettingJson());
                String type = root.path("type").asText();
                ComponentSpec spec = switch (type) {
                    case "EC2" -> ComponentSpec.newBuilder().setEC2(convertToEC2(root)).build();
                    case "MySQL" -> ComponentSpec.newBuilder().setMySQL(convertToMySQL(root)).build();
                    case "Spring" -> ComponentSpec.newBuilder().setSpring(convertToSpring(root)).build();
                    case "React" -> ComponentSpec.newBuilder().setReact(convertToReact(root)).build();
                    case "S3" -> ComponentSpec.newBuilder().setS3(convertToS3(root)).build();
                    default -> throw new IllegalArgumentException("지원하지 않는 컴포넌트 타입: " + type);
                };
                componentSpecs.add(spec);
            } catch (Exception e) {
                sendToEmitter(emitter, "deploy-error", "설정 파싱 실패: " + e.getMessage());
                emitter.completeWithError(e);
                return emitter;
            }
        }

        DeploymentRequest request = DeploymentRequest.newBuilder()
            .setDeploymentId(deploymentId)
            .addAllComponents(componentSpecs)
            .build();

        // PlanDeploy 선 호출
        try {
            System.out.println(">>> BEFORE PlanDeploy");
            CostResponse costResponse = stub.planDeploy(request);
            System.out.println(">>> AFTER PlanDeploy");
            System.out.println("예상 비용: " + costResponse.getCost());
            // 필요시 SSE로 예상 비용 전달
            sendToEmitter(emitter, "deploy-plan", "예상 비용: " + costResponse.getCost());
        } catch (Exception e) {
            e.printStackTrace();
            sendToEmitter(emitter, "deploy-fail", "PlanDeploy 실패: " + e.getMessage());
            emitter.completeWithError(e);
            return emitter;
        }


        deploymentExecutor.execute(() -> {
            try {
                System.out.println(">>> Before gRPC call");
                stub.deploy(request).forEachRemaining(log ->
                    sendToEmitter(emitter, "deploy-log", log.getLogLine())
                );
                System.out.println(">>> After gRPC call");
                sendToEmitter(emitter, "deploy-success", "배포가 성공적으로 완료되었습니다.");
                emitter.complete();

                components.forEach(component ->
                    componentStatusStoreService.upsert(component.getComponentId().toString(), ComponentStatus.RUNNING)
                );

            } catch (Exception e) {
                sendToEmitter(emitter, "deploy-fail", "배포 중 오류 발생: " + e.getMessage());
                emitter.completeWithError(e);

                components.forEach(component ->
                    componentStatusStoreService.upsert(component.getComponentId().toString(), ComponentStatus.ERROR)
                );
            }
        });

        return emitter;
    }

    // @Transactional
    // public SseEmitter startDeployment(Long projectId) {
    //     SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30분 제한

    //     // 1. 프로젝트 조회
    //     Project project = projectRepository.findById(projectId)
    //         .orElseThrow(ProjectNotFoundException::new);

    //     // 2. 컴포넌트 조회
    //     List<Component> components = componentRepository.findByProject(project);

    //     // 3. 컴포넌트 상태 검증
    //     // TODO : 지금은 컴포넌트의 상태가 START이지만 아마 READY가 추가될 수도 있음.
    //     boolean allStart = components.stream().allMatch(component -> {
    //         return componentStatusRepository
    //             .findByComponentId(component.getComponentId().toString())
    //             .map(doc -> ComponentStatus.START.equals(doc.getStatus()))
    //             .orElse(false);
    //     });

    //     if (!allStart) {
    //         sendToEmitter(emitter, "deploy-error", "START 상태가 아닌 컴포넌트가 존재합니다. 설정을 확인해주세요.");
    //         emitter.complete();
    //         return emitter;
    //     }

    //     /*
    //         4. Deployment ID 생성
    //         RDB의 deploymentId : DB의 기본키 역할
    //         gRPC 전달용 String deploymentId : 외부 API / 로그 추적
    //     */ 
    //     String deploymentId = "deploy-" + projectId + "-" + System.currentTimeMillis();

    //     // 4.1 Deployment 엔티티 저장 (RDB)
    //     Deployment deployment = Deployment.builder()
    //         .project(project)
    //         .status("START")
    //         .startTime(LocalDateTime.now())
    //         .build();
    //     deploymentRepository.save(deployment);

    //     ComponentSpec.Builder builder = ComponentSpec.newBuilder();

    //     for (Component component : components) {
    //         ComponentSettingDocument settingDoc = componentSettingRepository.findByComponentId(component.getComponentId())
    //             .orElseThrow(() -> new CustomException("ComponentId " + component.getComponentId() + " 의 설정 정보가 존재하지 않습니다.", HttpStatus.BAD_REQUEST));

    //         JsonNode root = objectMapper.readTree(settingDoc.getSettingJson());
    //         String type = root.path("type").asText();

    //         switch (type) {
    //             case "EC2" -> builder.setEC2(convertToEC2(root));
    //             case "MySQL" -> builder.setMySQL(convertToMySQL(root));
    //             case "Spring" -> builder.setSpring(convertToSpring(root));
    //             case "React" -> builder.setReact(convertToReact(root));
    //             case "S3" -> builder.setS3(convertToS3(root));
    //             default -> throw new IllegalArgumentException("지원하지 않는 컴포넌트 타입: " + type);
    //         }
    //     }

    //     List<ComponentSpec> componentSpecs = List.of(builder.build());

    //     DeploymentRequest request = DeploymentRequest.newBuilder()
    //         .setDeploymentId(deploymentId)
    //         .addAllComponents(componentSpecs) // 길이 1의 리스트
    //         .build();


    //     // // 5. ComponentSetting → gRPC 메시지 변환
    //     // List<ComponentSpec> componentSpecs = components.stream()
    //     //     .map(component -> {
    //     //         ComponentSettingDocument settingDoc = componentSettingRepository.findByComponentId(component.getComponentId())
    //     //             .orElseThrow(() -> new CustomException("ComponentId " + component.getComponentId() + " 의 설정 정보가 존재하지 않습니다.", HttpStatus.BAD_REQUEST));
    //     //         return convertSettingJsonToComponentSpec(settingDoc.getSettingJson());
    //     //     })
    //     //     .toList();

    //     // // 6. gRPC 요청 생성
    //     // DeploymentRequest request = DeploymentRequest.newBuilder()
    //     //     .setDeploymentId(deploymentId)
    //     //     .addAllComponents(componentSpecs)
    //     //     .build();

    //     // 7. 비동기 배포 실행 + 로그 스트리밍
    //     deploymentExecutor.execute(() -> {
    //         try {
    //             stub.deploy(request).forEachRemaining(log -> {
    //                 sendToEmitter(emitter, "deploy-log", log.getLogLine());
    //             });

    //             sendToEmitter(emitter, "deploy-success", "배포가 성공적으로 완료되었습니다.");
    //             emitter.complete();

    //             // TODO: ComponentStatus → MongoDB에 RUNNING 상태로 반영 필요
    //             components.forEach(component -> {
    //                 componentStatusStoreService.upsert(
    //                     component.getComponentId().toString(),
    //                     ComponentStatus.RUNNING
    //                 );
    //             });

    //         } catch (Exception e) {
    //             sendToEmitter(emitter, "deploy-fail", "배포 중 오류 발생: " + e.getMessage());
    //             emitter.completeWithError(e);

    //             // TODO: 실패한 경우 전체 Component 상태 ERROR 처리 필요
    //             components.forEach(component -> {
    //                 componentStatusStoreService.upsert(
    //                     component.getComponentId().toString(),
    //                     ComponentStatus.ERROR
    //                 );
    //             });
    //         }
    //     });

    //     return emitter;
    // }




    // public void restartDeployment(Long projectId) {
    //     // TODO: 실제 배포 재시작 로직 구현 
    //     // 컴포넌트 하나라도 죽으면 전체 실행 불가로 MVP를 만들거라 MVP에서는 구현하지 않을 예정
    // }

    @Transactional
    public void stopDeployment(Long projectId) {
        // 1. 프로젝트 조회
        Project project = projectRepository.findById(projectId)
            .orElseThrow(ProjectNotFoundException::new);

        // TODO : 2. "실행 중인 배포" 조회 (명확한 상태 필터링 필요)
        Deployment deployment = deploymentRepository.findByProjectAndStatus(project, "RUNNING")
            .orElseThrow(() -> new CustomException("실행 중인 배포가 없습니다.", HttpStatus.BAD_REQUEST));

        String deploymentId = "deploy-" + projectId + "-" + deployment.getDeploymentId();

        // 3. 프로젝트 내 컴포넌트 조회
        List<Component> components = componentRepository.findByProject(project);

        if (components.isEmpty()) {
            throw new CustomException("중단할 컴포넌트가 없습니다.", HttpStatus.BAD_REQUEST);
        }

        // 4. ComponentSpec 생성
        List<ComponentSpec> componentSpecs = new ArrayList<>();
        for (Component component : components) {
            ComponentSettingDocument settingDoc = componentSettingRepository.findByComponentId(component.getComponentId())
                .orElseThrow(() -> new CustomException("설정 누락: " + component.getComponentId(), HttpStatus.BAD_REQUEST));

            try {
                JsonNode root = objectMapper.readTree(settingDoc.getSettingJson());
                String type = root.path("type").asText();

                ComponentSpec spec = switch (type) {
                    case "EC2" -> ComponentSpec.newBuilder().setEC2(convertToEC2(root)).build();
                    case "S3" -> ComponentSpec.newBuilder().setS3(convertToS3(root)).build();
                    case "Spring" -> ComponentSpec.newBuilder().setSpring(convertToSpring(root)).build();
                    case "React" -> ComponentSpec.newBuilder().setReact(convertToReact(root)).build();
                    case "MySQL" -> ComponentSpec.newBuilder().setMySQL(convertToMySQL(root)).build();
                    default -> throw new IllegalArgumentException("지원하지 않는 타입: " + type);
                };

                componentSpecs.add(spec);

            } catch (Exception e) {
                throw new CustomException("설정 파싱 오류: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // 5. gRPC 요청 전송
        DeploymentRequest request = DeploymentRequest.newBuilder()
            .setDeploymentId(deploymentId)
            .addAllComponents(componentSpecs)
            .build();

        try {
            stub.stopDeploy(request); // 예외 발생 시 catch
            for (Component component : components) {
                componentStatusStoreService.upsert(component.getComponentId().toString(), ComponentStatus.START);
            }
            
            deployment.markAsStart(LocalDateTime.now());
            deploymentRepository.save(deployment);
        
        } catch (Exception e) {
            // 실패한 경우 전체를 ERROR로 간주
            for (Component component : components) {
                componentStatusStoreService.upsert(component.getComponentId().toString(), ComponentStatus.ERROR);
            }

            deployment.markAsError(LocalDateTime.now());
            deploymentRepository.save(deployment);

            throw new CustomException("배포 중단 실패: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




    public CheckDto checkDeploymentPreconditions(Long projectId) {
        // TODO: 사전 체크 로직 구현
        return null;
    }

    public ChargeEstimateDto planDeployment(Long projectId) {
        // TODO: 예상 요금 계산 로직 구현
        return null;
    }

    public ChargeResultDto getCharge(Long projectId) {
        // TODO: 실제 요금 조회 로직 구현
        return null;
    }

    public SseEmitter getDeploymentStatus(Long projectId) {
        // TODO: SSE로 상태 업데이트 전송
        SseEmitter emitter = new SseEmitter();
        // 예시: emitter.send("STATUS: RUNNING");
        return emitter;
    }

    // public SseEmitter streamDeploymentLogs(Long projectId) {
    //     // TODO: SSE로 로그 스트리밍 구현
    //     SseEmitter emitter = new SseEmitter();
    //     // 예시: emitter.send("[INFO] 서비스 시작 중...");
    //     return emitter;
    // }

    private ComponentSpec convertSettingJsonToComponentSpec(String settingJson) {
        try {
            JsonNode root = objectMapper.readTree(settingJson);
            String type = root.path("type").asText();
    
            ComponentSpec.Builder builder = ComponentSpec.newBuilder();
    
            switch (type) {
                case "EC2" -> {
                    EC2Request ec2 = EC2Request.newBuilder()
                        .setInstanceType(root.path("InstanceType").asText())
                        .setRegion(root.path("Region").asText())
                        .setAMI(root.path("AMI").asText())
                        .setInstanceName(root.path("InstanceName").asText())
                        .addAllOpenPorts(objectMapper.convertValue(
                            root.path("OpenPorts"), new TypeReference<List<Integer>>() {}))
                        .setAWSAccessKey(root.path("AWSAccessKey").asText())
                        .setAWSSecretKey(root.path("AWSSecretKey").asText())
                        .setComponentId(root.path("ComponentId").asText())
                        .build();
                    builder.setEC2(ec2);
                }
    
                case "MySQL" -> {
                    MySQLRequest mysql = MySQLRequest.newBuilder()
                        .setMySQLRootPassword(root.path("MySQLRootPassword").asText())
                        .setMySQLDatabase(root.path("MySQLDatabase").asText())
                        .setMySQLUser(root.path("MySQLUser").asText())
                        .setMySQLPassword(root.path("MySQLPassword").asText())
                        .setPort(root.path("Port").asInt())
                        .setParentComponentId(root.path("ParentComponentId").asText())
                        .setComponentId(root.path("ComponentId").asText())
                        .build();
                    builder.setMySQL(mysql);
                }
    
                case "Spring", "React" -> {
                    JsonNode gitNode = root.path("GitCredential");
                    String idText = gitNode.path("Id").asText();
                    String keyText = gitNode.path("Key").asText();
    
                    if (idText == null || idText.isBlank()) {
                        throw new CustomException("GitCredential ID가 비어 있습니다.", HttpStatus.BAD_REQUEST);
                    }
    
                    Long credentialId = Long.parseLong(idText);
                    Credential credential = credentialRepository.findById(credentialId)
                        .orElseThrow(InvalidCredentialIdException::new);
    
                    GitCredential gitCredential = GitCredential.newBuilder()
                        .setId(idText)
                        .setKey(keyText)
                        .build();
    
                    if (type.equals("Spring")) {
                        SpringRequest spring = SpringRequest.newBuilder()
                            .setParentComponentId(root.path("ParentComponentId").asText())
                            .setGitRepo(root.path("GitRepo").asText())
                            .setNginxPort(root.path("NginxPort").asInt())
                            .setBuildTool(root.path("BuildTool").asText())
                            .setJDKVersion(root.path("JDKVersion").asText())
                            .setDockerPort(root.path("DockerPort").asInt())
                            .setComponentId(root.path("ComponentId").asText())
                            .setGitCredential(gitCredential)
                            .build();
                        builder.setSpring(spring);
                    } else {
                        ReactRequest react = ReactRequest.newBuilder()
                            .setParentComponentId(root.path("ParentComponentId").asText())
                            .setGitRepo(root.path("GitRepo").asText())
                            .setComponentId(root.path("ComponentId").asText())
                            .setGitCredential(gitCredential)
                            .build();
                        builder.setReact(react);
                    }
                }
    
                case "S3" -> {
                    S3Request s3 = S3Request.newBuilder()
                        .setBucketName(root.path("BucketName").asText())
                        .setRegion(root.path("Region").asText())
                        .setAWSAccessKey(root.path("AWSAccessKey").asText())
                        .setAWSSecretKey(root.path("AWSSecretKey").asText())
                        .setComponentId(root.path("ComponentId").asText())
                        .build();
                    builder.setS3(s3);
                }
    
                default -> throw new IllegalArgumentException("지원하지 않는 컴포넌트 타입: " + type);
            }
    
            return builder.build();
    
        } catch (Exception e) {
            throw new RuntimeException("gRPC ComponentSpec 변환 실패", e);
        }
    }
    

    private void sendToEmitter(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private EC2Request convertToEC2(JsonNode root) {
        String rawComponentId = root.path("ComponentId").asText(); // main.tf.tpl에서 숫자 먼저 사용하지 않으려고

        return EC2Request.newBuilder()
            .setInstanceType(root.path("InstanceType").asText())
            .setRegion(root.path("Region").asText())
            .setAMI(root.path("AMI").asText())
            .setInstanceName(root.path("InstanceName").asText())
            .addAllOpenPorts(objectMapper.convertValue(
                root.path("OpenPorts"), new TypeReference<List<Integer>>() {}))
            .setAWSAccessKey(root.path("AWSAccessKey").asText())
            .setAWSSecretKey(root.path("AWSSecretKey").asText())
            .setComponentId("comp-" + rawComponentId) // main.tf.tpl에서 숫자 먼저 사용하지 않으려고
            .build();
    }

    private MySQLRequest convertToMySQL(JsonNode root) {
        String rawComponentId = root.path("ComponentId").asText();

        return MySQLRequest.newBuilder()
            .setMySQLRootPassword(root.path("MySQLRootPassword").asText())
            .setMySQLDatabase(root.path("MySQLDatabase").asText())
            .setMySQLUser(root.path("MySQLUser").asText())
            .setMySQLPassword(root.path("MySQLPassword").asText())
            .setPort(root.path("Port").asInt())
            .setParentComponentId("comp-" + root.path("ParentComponentId").asText()) 
            .setComponentId("comp-" + rawComponentId)  
            .build();
    }
    
    private S3Request convertToS3(JsonNode root) {
        String rawComponentId = root.path("ComponentId").asText();
        String deploymentId = root.path("DeploymentId").asText(); // 409 BucketAlreadyExists 해결, S3는 글로벌 고유 

        return S3Request.newBuilder()
            .setBucketName("pado-" + deploymentId + "-" + rawComponentId) // 409 BucketAlreadyExists 해결, S3는 글로벌 고유 
            .setRegion(root.path("Region").asText())
            .setAWSAccessKey(root.path("AWSAccessKey").asText())
            .setAWSSecretKey(root.path("AWSSecretKey").asText())
            .setComponentId("comp-" + rawComponentId) 
            .build();
    }
    
    private SpringRequest convertToSpring(JsonNode root) {
        String rawComponentId = root.path("ComponentId").asText();

        JsonNode gitNode = root.path("GitCredential");
        String idText = gitNode.path("Id").asText();
        String keyText = gitNode.path("Key").asText();
    
        if (idText == null || idText.isBlank()) {
            throw new CustomException("GitCredential ID가 비어 있습니다.", HttpStatus.BAD_REQUEST);
        }
    
        Long credentialId = Long.parseLong(idText);
        Credential credential = credentialRepository.findById(credentialId)
            .orElseThrow(InvalidCredentialIdException::new);
    
        GitCredential gitCredential = GitCredential.newBuilder()
            .setId(idText)
            .setKey(keyText)
            .build();
    
        return SpringRequest.newBuilder()
            .setParentComponentId("comp-" + root.path("ParentComponentId").asText())
            .setGitRepo(root.path("GitRepo").asText())
            .setNginxPort(root.path("NginxPort").asInt())
            .setBuildTool(root.path("BuildTool").asText())
            .setJDKVersion(root.path("JDKVersion").asText())
            .setDockerPort(root.path("DockerPort").asInt())
            .setComponentId("comp-" + rawComponentId)
            .setGitCredential(gitCredential)
            .build();
    }
    
    private ReactRequest convertToReact(JsonNode root) {
        String rawComponentId = root.path("ComponentId").asText();

        JsonNode gitNode = root.path("GitCredential");
        String idText = gitNode.path("Id").asText();
        String keyText = gitNode.path("Key").asText();
    
        if (idText == null || idText.isBlank()) {
            throw new CustomException("GitCredential ID가 비어 있습니다.", HttpStatus.BAD_REQUEST);
        }
    
        Long credentialId = Long.parseLong(idText);
        Credential credential = credentialRepository.findById(credentialId)
            .orElseThrow(InvalidCredentialIdException::new);
    
        GitCredential gitCredential = GitCredential.newBuilder()
            .setId(idText)
            .setKey(keyText)
            .build();
    
        return ReactRequest.newBuilder()
            .setParentComponentId("comp-" + root.path("ParentComponentId").asText())
            .setGitRepo(root.path("GitRepo").asText())
            .setComponentId("comp-" + rawComponentId)
            .setGitCredential(gitCredential)
            .build();
    }
    
    private GitCredential extractGitCredential(JsonNode root) {
        JsonNode gitNode = root.path("GitCredential");
        String idText = gitNode.path("Id").asText();
        String keyText = gitNode.path("Key").asText();

        if (idText == null || idText.isBlank()) {
            throw new CustomException("GitCredential ID가 비어 있습니다.", HttpStatus.BAD_REQUEST);
        }

        Long credentialId = Long.parseLong(idText);
        credentialRepository.findById(credentialId)
            .orElseThrow(InvalidCredentialIdException::new);

        return GitCredential.newBuilder().setId(idText).setKey(keyText).build();
    }

}

