package com.pado.backend.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pado.backend.domain.Component;
import com.pado.backend.domain.Deployment;
import com.pado.backend.domain.Project;
import com.pado.backend.domain.mongo.ComponentSettingDocument;
import com.pado.backend.domain.mongo.ComponentStatusDocument;
import com.pado.backend.dto.response.ChargeEstimateDto;
import com.pado.backend.dto.response.ChargeResultDto;
import com.pado.backend.dto.response.CheckDto;
import com.pado.backend.global.exception.CustomException;
import com.pado.backend.global.exception.ProjectNotFoundException;
import com.pado.backend.global.type.ComponentStatus;
import com.pado.backend.repository.ComponentLinkRepository;
import com.pado.backend.repository.ComponentRepository;
import com.pado.backend.repository.DeploymentRepository;
import com.pado.backend.repository.ProjectRepository;
import com.pado.backend.repository.mongo.ComponentSettingRepository;
import com.pado.backend.repository.mongo.ComponentStatusRepository;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.RequiredArgsConstructor;
import provision.Provisioning.ComponentSpec;
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

    private final ComponentSettingRepository componentSettingRepository;
    private final ComponentStatusRepository componentStatusRepository;

    // gRPC
    private final ObjectMapper objectMapper;
    private final Executor deploymentExecutor;
    private final ProvisioningServiceGrpc.ProvisioningServiceBlockingStub stub;

    @Transactional
    public SseEmitter startDeployment(Long projectId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30분 제한

        // 1. 프로젝트 조회
        Project project = projectRepository.findById(projectId)
            .orElseThrow(ProjectNotFoundException::new);

        // 2. 컴포넌트 조회
        List<Component> components = componentRepository.findByProject(project);

        // 3. 컴포넌트 상태 검증
        // TODO : 지금은 컴포넌트의 상태가 START이지만 아마 READY가 추가될 수도 있음.
        boolean allStart = components.stream().allMatch(component ->
            componentStatusRepository
                .findLatestStatus(component.getComponentId().toString())
                .map(status -> ComponentStatus.START.equals(status.getStatus()))
                .orElse(false)
        );

        if (!allStart) {
            sendToEmitter(emitter, "deploy-error", "START 상태가 아닌 컴포넌트가 존재합니다. 설정을 확인해주세요.");
            emitter.complete();
            return emitter;
        }

        /*
            4. Deployment ID 생성
            RDB의 deploymentId : DB의 기본키 역할
            gRPC 전달용 String deploymentId : 외부 API / 로그 추적
        */ 
        String deploymentId = "deploy-" + projectId + "-" + System.currentTimeMillis();

        // 4.1 Deployment 엔티티 저장 (RDB)
        Deployment deployment = Deployment.builder()
            .project(project)
            .status("START")
            .startTime(LocalDateTime.now())
            .build();
        deploymentRepository.save(deployment);

        // 5. ComponentSetting → gRPC 메시지 변환
        List<ComponentSpec> componentSpecs = components.stream()
            .map(component -> {
                ComponentSettingDocument settingDoc = componentSettingRepository.findByComponentId(component.getComponentId())
                    .orElseThrow(() -> new CustomException("ComponentId " + component.getComponentId() + " 의 설정 정보가 존재하지 않습니다.", HttpStatus.BAD_REQUEST));
                return convertSettingJsonToComponentSpec(settingDoc.getSettingJson());
            })
            .toList();

        // 6. gRPC 요청 생성
        DeploymentRequest request = DeploymentRequest.newBuilder()
            .setDeploymentId(deploymentId)
            .addAllComponents(componentSpecs)
            .build();

        // 7. 비동기 배포 실행 + 로그 스트리밍
        deploymentExecutor.execute(() -> {
            try {
                stub.deploy(request).forEachRemaining(log -> {
                    sendToEmitter(emitter, "deploy-log", log.getLogLine());
                });

                sendToEmitter(emitter, "deploy-success", "배포가 성공적으로 완료되었습니다.");
                emitter.complete();

                // TODO: ComponentStatus → MongoDB에 RUNNING 상태로 반영 필요
                components.forEach(component -> {
                    ComponentStatusDocument updated = ComponentStatusDocument.builder()
                        .componentId(component.getComponentId().toString())
                        .deploymentId(deploymentId)
                        .status(ComponentStatus.RUNNING)
                        .updatedAt(LocalDateTime.now())
                        .build();
                    componentStatusRepository.save(updated);
                });

                // deployment.setStatus("RUNNING");
                // deployment.setStopTime(LocalDateTime.now());
                // deploymentRepository.save(deployment);
            } catch (Exception e) {
                sendToEmitter(emitter, "deploy-fail", "배포 중 오류 발생: " + e.getMessage());
                emitter.completeWithError(e);

                // TODO: 실패한 경우 전체 Component 상태 ERROR 처리 필요
                components.forEach(component -> {
                    ComponentStatusDocument updated = ComponentStatusDocument.builder()
                        .componentId(component.getComponentId().toString())
                        .deploymentId(deploymentId)
                        .status(ComponentStatus.ERROR)
                        .updatedAt(LocalDateTime.now())
                        .build();
                    componentStatusRepository.save(updated);
                });
            }
        });

        return emitter;
    }




    // public void restartDeployment(Long projectId) {
    //     // TODO: 실제 배포 재시작 로직 구현 
    //     // 컴포넌트 하나라도 죽으면 전체 실행 불가로 MVP를 만들거라 MVP에서는 구현하지 않을 예정
    // }

    public void stopDeployment(Long projectId) {
        // TODO: 실제 배포 중지 로직 구현
    }

    public CheckDto checkDeploymentPreconditions(Long projectId) {
        // TODO: 사전 체크 로직 구현
        return null;
    }

    public ChargeEstimateDto estimateCharge(Long projectId) {
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
    
            String type = root.path("type").asText();  // 예: "EC2", "Spring", ...
    
            switch (type) {
                case "EC2" -> {
                    EC2Request ec2 = EC2Request.newBuilder()
                        .setInstanceType(root.path("InstanceType").asText())
                        .setRegion(root.path("Region").asText())
                        .setAMI(root.path("AMI").asText())
                        .setInstanceName(root.path("InstanceName").asText())
                        .addAllOpenPorts(objectMapper.convertValue(root.path("OpenPorts"), new TypeReference<List<Integer>>() {}))
                        .setAWSAccessKey(root.path("AWSAccessKey").asText())
                        .setAWSSecretKey(root.path("AWSSecretKey").asText())
                        .setComponentId(root.path("ComponentId").asText())
                        .build();
                    return ComponentSpec.newBuilder().setEC2(ec2).build();
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
                    return ComponentSpec.newBuilder().setMySQL(mysql).build();
                }
                case "Spring" -> {
                    Map<String, String> env = objectMapper.convertValue(
                        root.path("Env"), new TypeReference<Map<String, String>>() {}
                    );
                    SpringRequest spring = SpringRequest.newBuilder()
                        .setParentComponentId(root.path("ParentComponentId").asText())
                        .setGitRepo(root.path("GitRepo").asText())
                        .setNginxPort(root.path("NginxPort").asInt())
                        .setBuildTool(root.path("BuildTool").asText())
                        .setJDKVersion(root.path("JDKVersion").asText())
                        .putAllEnv(env)
                        .setDockerPort(root.path("DockerPort").asInt())
                        .setComponentId(root.path("ComponentId").asText())
                        .setGitCredential(
                            GitCredential.newBuilder()
                                .setId(root.path("GitCredential").path("Id").asText())
                                .setKey(root.path("GitCredential").path("Key").asText())
                                .build()
                        )
                        .build();
                    return ComponentSpec.newBuilder().setSpring(spring).build();
                }
                case "React" -> {
                    ReactRequest react = ReactRequest.newBuilder()
                        .setParentComponentId(root.path("ParentComponentId").asText())
                        .setGitRepo(root.path("GitRepo").asText())
                        .setComponentId(root.path("ComponentId").asText())
                        .setGitCredential(
                            GitCredential.newBuilder()
                                .setId(root.path("GitCredential").path("Id").asText())
                                .setKey(root.path("GitCredential").path("Key").asText())
                                .build()
                        )
                        .build();
                    return ComponentSpec.newBuilder().setReact(react).build();
                }
                case "S3" -> {
                    S3Request s3 = S3Request.newBuilder()
                        .setBucketName(root.path("BucketName").asText())
                        .setRegion(root.path("Region").asText())
                        .setAWSAccessKey(root.path("AWSAccessKey").asText())
                        .setAWSSecretKey(root.path("AWSSecretKey").asText())
                        .setComponentId(root.path("ComponentId").asText())
                        .build();
                    return ComponentSpec.newBuilder().setS3(s3).build();
                }
                default -> throw new IllegalArgumentException("지원하지 않는 컴포넌트 타입: " + type);
            }
    
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
}

