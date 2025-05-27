package com.pado.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pado.backend.domain.Component;
import com.pado.backend.domain.ComponentLink;
import com.pado.backend.domain.ComponentMeta;
import com.pado.backend.domain.Project;
import com.pado.backend.domain.mongo.ComponentSettingDocument;
import com.pado.backend.domain.mongo.ComponentStatusDocument;
import com.pado.backend.dto.request.ComponentConnectDto;
import com.pado.backend.dto.request.ComponentCreateRequestDto;
import com.pado.backend.dto.request.ComponentSettingDto;
import com.pado.backend.dto.response.ComponentDetailDto;
import com.pado.backend.dto.response.ComponentInfo;
import com.pado.backend.dto.response.ComponentSearchDto;
import com.pado.backend.dto.response.ComponentServiceUrlDto;
import com.pado.backend.dto.response.ComponentTemplate;
import com.pado.backend.dto.response.ComponentTypeDto;
import com.pado.backend.dto.response.DefaultResponseDto;
import com.pado.backend.global.exception.ComponentDeletionNotAllowedException;
import com.pado.backend.global.exception.ComponentNotFoundException;
import com.pado.backend.global.exception.ComponentProjectMismatchException;
import com.pado.backend.global.exception.CustomException;
import com.pado.backend.global.exception.InvalidComponentRequestException;
import com.pado.backend.global.exception.InvalidJsonFormatException;
import com.pado.backend.global.exception.ProjectNotFoundException;
import com.pado.backend.global.exception.UnauthorizedComponentAccessException;
import com.pado.backend.global.type.ComponentStatus;
import com.pado.backend.repository.ComponentLinkRepository;
import com.pado.backend.repository.ComponentMetaRepository;
import com.pado.backend.repository.ComponentRepository;
import com.pado.backend.repository.ProjectRepository;
import com.pado.backend.repository.mongo.ComponentSettingRepository;
import com.pado.backend.repository.mongo.ComponentStatusRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComponentService {

    private final ComponentRepository componentRepository;
    private final ComponentLinkRepository componentLinkRepository;
    private final ComponentStatusRepository componentStatusRepository;
    private final ProjectRepository projectRepository;
    private final ComponentMetaRepository componentMetaRepository;
    // Mongo
    private final ComponentSettingRepository componentSettingRepository;
    private final ComponentStatusStoreService componentStatusStoreService;

    // 컴포넌트 종류 조회
    public List<ComponentTypeDto> getComponentTypes() {
        return componentMetaRepository.findAll().stream()
        .map(meta -> new ComponentTypeDto(meta.getSubtype(), meta.getThumbnail()))
        .collect(Collectors.toList());
    }

    
    // [x] : 컴포넌트 검색 
    public List<ComponentSearchDto> searchComponents(String keyword) {
        List<ComponentMeta> componentMetas;

        if (keyword == null) {
            // null이면 전체 조회
            componentMetas = componentMetaRepository.findAll();
        } else if (keyword.trim().isEmpty()) {
            // 공백 문자열이면 명시적 방어: 빈 리스트 반환
            return List.of();
        } else {
            // keyword가 입력되었을 경우 type 또는 subtype에서 검색
            componentMetas = componentMetaRepository.searchByKeyword(keyword);
        }

        return componentMetas.stream()
            .map(meta -> new ComponentSearchDto(
                new ComponentTemplate(meta.getType(), meta.getSubtype(), meta.getThumbnail())
            ))
            .collect(Collectors.toList());
    }

    // 컴포넌트 배치
    // TODO :createdAt, updatedAt 자동화(Audit)
    @Transactional
    public ComponentDetailDto createComponentToProject(Long projectId, ComponentCreateRequestDto request) {
        // 1. 프로젝트 조회
        Project project = projectRepository.findById(projectId)
            .orElseThrow(ProjectNotFoundException::new);

        ComponentCreateRequestDto.ComponentInfo info = request.getComponent();

        // 2. 부모 컴포넌트 조회 (SERVICE 타입일 경우 필수)
        Component parent = null;
        if ("SERVICE".equalsIgnoreCase(info.getType())) {
            if (info.getParentComponentId() == null) {
                throw new InvalidComponentRequestException();
            }
            parent = componentRepository.findById(info.getParentComponentId())
                .orElseThrow(ComponentNotFoundException::new);
        }
        
        // 3. 컴포넌트 생성
        // [ ] : 컴포넌트 이름 우리가 작성?
        Component component = Component.builder()
            .componentName(info.getType() + "-" + info.getSubtype() + "-" + UUID.randomUUID().toString().substring(0, 6))
            .type(info.getType())
            .subtype(info.getSubtype())
            .thumbnail(info.getThumbnail())
            .project(project)
            .parentComponentId(parent)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // 컴포넌트 저장
        Component saved = componentRepository.save(component);

        // 4. 상태 저장 (MongoDB, 덮어쓰기)
        componentStatusStoreService.upsert(
            saved.getComponentId().toString(),
            ComponentStatus.DRAFT
        );

        // 5. 응답 DTO 생성
        ComponentInfo response;

        if ("RESOURCE".equalsIgnoreCase(saved.getType())) {
            // RESOURCE 타입 → 단독 반환 (ownedServices 없음)
            response = new ComponentInfo(
                saved.getComponentId(),
                saved.getType(),
                saved.getSubtype(),
                saved.getThumbnail(),
                ComponentStatus.DRAFT,
                null,   // parentComponentId 없음
                null,   // ownedServices 없음
                List.of()
            );
        } else {
            // SERVICE 타입 → 자신의 부모(RESOURCE)의 ownedServices 리스트 갱신
            List<Component> childServices = componentRepository.findByParentComponentId(
                parent);

            List<ComponentInfo.OwnedService> ownedServices = childServices.stream()
                .map(service -> new ComponentInfo.OwnedService(
                    service.getComponentId(),
                    service.getSubtype(),
                    ComponentStatus.DRAFT
                ))
                .toList();

            response = new ComponentInfo(
                parent.getComponentId(),  // RESOURCE 기준으로 반환
                parent.getType(),
                parent.getSubtype(),
                parent.getThumbnail(),
                ComponentStatus.DRAFT,
                null, // parentComponentId는 RESOURCE니까 null
                ownedServices, // 자식 SERVICE 목록 포함
                List.of()
            );
        }

        return new ComponentDetailDto(response);
    }

    // [x] 컴포넌트 설정 적용
    @Transactional
    public DefaultResponseDto applyComponentSetting(Long projectId, Long componentId, ComponentSettingDto request) {
        // 1. 프로젝트 조회
        Project project = projectRepository.findById(projectId)
            .orElseThrow(ProjectNotFoundException::new);

        // 2. 컴포넌트 조회
        Component component = componentRepository.findById(componentId)
            .orElseThrow(ComponentNotFoundException::new);

        // 3. 프로젝트 ID 일치 여부 검증
        if (!component.getProject().getProjectId().equals(project.getProjectId())) {
            throw new ComponentProjectMismatchException();
        }

       // 4. 컴포넌트 설정 저장 (MongoDB - 나중에 배포 시 사용됨)
        ComponentSettingDocument settingDocument = new ComponentSettingDocument(
            null, // id는 MongoDB에서 자동 생성
            componentId,
            request.getSettingJson()
        );
        componentSettingRepository.save(settingDocument);

        // 5. 설정 JSON 내부에서 DeploymentId 추출 (상태 기록용)
        ObjectMapper objectMapper = new ObjectMapper();
        String deploymentId;
        try {
            JsonNode root = objectMapper.readTree(request.getSettingJson());
            deploymentId = root.path("DeploymentId").asText(null);
        } catch (JsonProcessingException e) {
            throw new InvalidJsonFormatException();
        }

        // 6. 상태 START로 저장 (Mongo)
        componentStatusStoreService.upsert(
            componentId.toString(),
            ComponentStatus.START
        );

        // 7. 응답 반환
        return new DefaultResponseDto("설정 적용 완료");
    }

    // 컴포넌트 연결
    public void connectComponent(Long projectId, Long componentId, ComponentConnectDto request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'connectComponent'");
    }

    // TODO : 컴포넌트 서비스 접속 , 일단 제외하고 배포, 생성부터
    public ComponentServiceUrlDto getComponentServiceUrl(Long projectId, Long componentId) {
        
        // 1. Component 조회
        Component component = componentRepository.findById(componentId)
        .orElseThrow(ComponentNotFoundException::new);

        // 2. Go에게 넘기는 형식의 componentId는 String (예: "102")
        String componentIdForGo = component.getComponentId().toString();

        // 3. 최신 상태 조회
        ComponentStatus status = componentStatusRepository.findByComponentId(componentIdForGo)
            .map(ComponentStatusDocument::getStatus)
            .orElse(ComponentStatus.ERROR);

        // TODO : url 구성 어떻게 할건지 생각
        // 4. URL 구성 (예: http://102.pado.local)
        String url = "http://" + componentIdForGo + ".pado.local";

        return new ComponentServiceUrlDto(url, status.name());
    }

    // [x] : 배치된 컴포넌트 검색
    public List<ComponentSearchDto> searchDeployedComponents(Long projectId, String keyword) {
        List<Component> components;

        Project project = projectRepository.findById(projectId)
        .orElseThrow(ProjectNotFoundException::new);

        if (keyword == null) {
            // keyword가 아예 없으면 전체 조회
            components = componentRepository.findByProject(project);
        } else if (keyword.trim().isEmpty()) {
            // 공백만 입력된 경우 → 빈 리스트 반환 (명시적 방어)
            return List.of();
        } else {
            // 정상 검색어 입력된 경우
            components = componentRepository.searchComponentsByProjectAndKeyword(projectId, keyword);
        }

        return components.stream()
            .map(c -> new ComponentSearchDto(
                new ComponentTemplate(c.getType(), c.getSubtype(), c.getThumbnail())
            ))
            .collect(Collectors.toList());
    }

    // [ ] : 개별 컴포넌트 상태 조회
    public SseEmitter getComponentStatus(Long projectId, Long componentId) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L); // 10분 지속 연결

        Runnable task = () -> {
            try {
                String componentIdStr = componentId.toString();

                // 일정 간격으로 상태 체크 (예: 3초마다)
                while (true) {
                    ComponentStatus status = componentStatusRepository.findByComponentId(componentIdStr)
                        .map(ComponentStatusDocument::getStatus)
                        .orElse(ComponentStatus.ERROR);

                    emitter.send(SseEmitter.event()
                        .name("component-status")
                        .data(status.name())
                        .id(componentIdStr)
                        .reconnectTime(3000));

                    Thread.sleep(3000); // 3초 대기 후 재송신
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        };

        new Thread(task).start();

        return emitter;
    }

    // 서비스 로그 모니터링
    public SseEmitter streamComponentLogs(Long projectId, Long componentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'streamComponentLogs'");
    }

    // 모니터링
    public SseEmitter streamMonitoring(Long projectId, Long componentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'streamMonitoring'");
    }

    // 컴포넌트 연결 해제
    public DefaultResponseDto disconnectComponent(Long projectId, Long componentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'disconnectComponent'");
    }

    /*  
        [x] : 컴포넌트 상태가 RUNNING이 아닌 경우에만 삭제 가능
        [x] : 삭제 전 후처리 (필요 시 고려할 것)
        자식 컴포넌트가 있는 경우 → 같이 삭제할지? (cascade 적용 여부) -> x 삭제하려는 컴포넌트만 삭제하고 링크 제거,
        다른 컴포넌트는 남아있어야함. 추후에 컴포넌트 생성해서 또 연결시킬 수 있기 때문

        1. 링크 없음 : 해당 컴포넌트만 삭제
        2. 링크 존재 : 링크 삭제 + 해당 컴포넌트 삭제 (연결된 상대 컴포넌트는 유지)
       [ ] : 실제 배포된 리소스가 있다면 → AWS/클러스터 등 외부 자원도 삭제해야 함 (지금은 생략해도 무방)
       [ ] : 관련된 모든 DB 정보 삭제도 필요할듯? MySQL, MongoDB에 기록되어 있던
     */
    @Transactional
    public DefaultResponseDto deleteComponent(Long projectId, Long componentId) {
        // 컴포넌트 조회
        Component component = componentRepository.findById(componentId)
            .orElseThrow(ComponentNotFoundException::new);

        // 프로젝트 소속 검증
        if (!component.getProject().getProjectId().equals(projectId)) {
            throw new UnauthorizedComponentAccessException();
        }

        //  MongoDB에서 최신 상태 조회
        Optional<ComponentStatusDocument> componentStatus =
            componentStatusRepository.findByComponentId(componentId.toString());

        // 상태가 RUNNING이면 예외(연결된 컴포넌트 제외, 삭제하려는 컴포넌트만)
        if (componentStatus.isPresent() && componentStatus.get().getStatus() == ComponentStatus.RUNNING) {
            throw new ComponentDeletionNotAllowedException();
        }

        // 연결된 링크가 있는지 확인하고 있으면 삭제
        List<ComponentLink> fromLinks = component.getFromLinks();
        List<ComponentLink> toLinks = component.getToLinks();

        if (!fromLinks.isEmpty()) {
            componentLinkRepository.deleteAll(fromLinks);
        }

        if (!toLinks.isEmpty()) {
            componentLinkRepository.deleteAll(toLinks);
        }

        // 삭제
        componentRepository.delete(component);

        return new DefaultResponseDto("컴포넌트 삭제 완료");
    }

    
}
