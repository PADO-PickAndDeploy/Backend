package com.pado.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pado.backend.domain.Component;
import com.pado.backend.domain.Project;
import com.pado.backend.domain.User;
import com.pado.backend.domain.mongo.ComponentStatusDocument;
import com.pado.backend.dto.request.ProjectCreateRequestDto;
import com.pado.backend.dto.response.ComponentInfo;
import com.pado.backend.dto.response.DefaultResponseDto;
import com.pado.backend.dto.response.ProjectDetailResponseDto;
import com.pado.backend.dto.response.ProjectResponseDto;
import com.pado.backend.global.exception.ProjectDeletionNotAllowedException;
import com.pado.backend.global.exception.ProjectNotFoundException;
import com.pado.backend.global.exception.UnauthorizedProjectAccessException;
import com.pado.backend.global.exception.UserNotFoundException;
import com.pado.backend.global.type.ComponentStatus;
import com.pado.backend.global.type.ProjectStatus;
import com.pado.backend.repository.ComponentRepository;
import com.pado.backend.repository.ProjectRepository;
import com.pado.backend.repository.UserRepository;
import com.pado.backend.repository.mongo.ComponentStatusRepository;

import lombok.RequiredArgsConstructor;

// TODO : 상태 관련 수정 필요
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ComponentRepository componentRepository;
    private final ComponentStatusRepository componentStatusRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional
    public ProjectResponseDto createProject(ProjectCreateRequestDto request, Long userId) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Project project = Project.builder()
                .projectName(request.getName())
                .projectDescription(request.getDescription())
                .user(user) // [x] 파라미터에 userid가 들어가야하지 않나? 필요하다 크리덴셜 서비스 - 크리덴셜 생성 처럼 , DTO에 넣을거냐 엔드포인트에 넣을거냐
                .build();

        Project saved = projectRepository.save(project);

        return new ProjectResponseDto(
                saved.getProjectId(),
                saved.getProjectName(),
                saved.getProjectDescription(),
                ProjectStatus.DRAFT, // 프로젝트 생성 시 배치된 컴포넌트가 없으므로 DRAFT 고정
                saved.getCreatedAt().format(formatter)
        );
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> getAllProjects(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return projectRepository.findByUser(user).stream()
        .map(project -> {
            ProjectStatus projectStatus = determineProjectStatus(project);
            return new ProjectResponseDto(
                project.getProjectId(),
                project.getProjectName(),
                project.getProjectDescription(),
                projectStatus,
                project.getCreatedAt().format(formatter)
            );
    })
    .collect(Collectors.toList());
    }

    // 프로젝트 개별 조회
    @Transactional(readOnly = true)
    public ProjectDetailResponseDto getProjectById(Long userId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(ProjectNotFoundException::new);

        if (!project.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedProjectAccessException();
        }

        // 1. 프로젝트에 연결된 컴포넌트 조회 + 각 컴포넌트의 상태 계산
        List<ComponentInfo> components = componentRepository.findByProject(project).stream()
                .map(component -> {
                    // 1-1. 동적으로 컴포넌트 상태 계산
                    ComponentStatus componentStatus = determineComponentStatus(component);

                    // 1-2. 소유 서비스 정보 매핑
                    List<ComponentInfo.OwnedService> ownedServices = component.getChildComponents().stream()
                            .map(child -> new ComponentInfo.OwnedService(
                                    child.getComponentId(),
                                    child.getSubtype(),
                                    determineComponentStatus(child)  // 자식 상태도 계산
                            ))
                            .toList();

                    // 1-3. 링크 정보 매핑
                    List<ComponentInfo.LinkInfo> links = component.getFromLinks().stream()
                            .map(link -> new ComponentInfo.LinkInfo(
                                    link.getToComponentId().getComponentId(),
                                    link.getConnectionType().name()
                            ))
                            .toList();

                    return new ComponentInfo(
                            component.getComponentId(),
                            component.getType(),
                            component.getSubtype(),
                            component.getThumbnail(),
                            componentStatus,
                            component.getParentComponentId() != null ? component.getParentComponentId().getComponentId() : null,
                            ownedServices.isEmpty() ? null : ownedServices,
                            links.isEmpty() ? null : links
                    );
                })
                .toList();

        // 2. 모든 컴포넌트가 RUNNING 상태인지 확인하여 프로젝트 상태 결정
        ProjectStatus projectStatus = determineProjectStatus(project);

        // 3. DTO 반환
        return new ProjectDetailResponseDto(
                project.getProjectId(),
                project.getProjectName(),
                projectStatus,
                project.getProjectDescription(),
                project.getCreatedAt().format(formatter),
                components
        );
    }

    @Transactional
    public DefaultResponseDto deleteProject(Long userId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(ProjectNotFoundException::new);
        ProjectStatus status = determineProjectStatus(project);

        if (status == ProjectStatus.RUNNING) {
            throw new ProjectDeletionNotAllowedException();
        }

        if (!project.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedProjectAccessException();
        }

        projectRepository.delete(project);
        return new DefaultResponseDto("프로젝트 삭제 완료");
    }

    private ComponentStatus determineComponentStatus(Component component) {
        return componentStatusRepository
                .findByComponentId(component.getComponentId().toString())
                .map(ComponentStatusDocument::getStatus)
                .orElse(ComponentStatus.DRAFT);
    }

    private ProjectStatus determineProjectStatus(Project project) {
        List<Component> components = componentRepository.findByProject(project);
    
        if (components.isEmpty()) return ProjectStatus.DRAFT;
    
        boolean allRunning = true;
        boolean anyError = false;
    
        for (Component c : components) {
            ComponentStatus status = determineComponentStatus(c);
            if (status == ComponentStatus.ERROR) {
                anyError = true;
            }
            if (status != ComponentStatus.RUNNING) {
                allRunning = false;
            }
        }
    
        if (anyError) return ProjectStatus.ERROR;
        if (allRunning) return ProjectStatus.RUNNING;
        return ProjectStatus.START;
    }

}
