package com.pado.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.pado.backend.domain.Project;
import com.pado.backend.domain.User;
import com.pado.backend.domain.Component;
import com.pado.backend.domain.mongo.ComponentStatusDocument;
import com.pado.backend.dto.request.ProjectCreateRequestDto;
import com.pado.backend.dto.response.ProjectDetailResponseDto;
import com.pado.backend.dto.response.ProjectResponseDto;
import com.pado.backend.global.exception.ProjectDeletionNotAllowedException;
import com.pado.backend.global.exception.ProjectNotFoundException;
import com.pado.backend.global.exception.UnauthorizedProjectAccessException;
import com.pado.backend.global.type.ComponentStatus;
import com.pado.backend.global.type.ProjectStatus;
import com.pado.backend.repository.ComponentRepository;
import com.pado.backend.repository.ProjectRepository;
import com.pado.backend.repository.UserRepository;
import com.pado.backend.repository.mongo.ComponentStatusRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class ProjectServiceTest {

    @InjectMocks
    private ProjectService projectService;

    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ComponentRepository componentRepository;
    @Mock private ComponentStatusRepository componentStatusRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("프로젝트 생성에 성공하면 DRAFT 상태를 반환한다")
    void createProject_success() {
        Long userId = 1L;
        User user = User.builder().userId(userId).build();
        ProjectCreateRequestDto request = new ProjectCreateRequestDto("Test Project", "설명");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectRepository.save(any(Project.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponseDto response = projectService.createProject(request, userId);

        assertThat(response.getName()).isEqualTo("Test Project");
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.DRAFT);
    }

    @Nested
    @DisplayName("deleteProject()")
    class DeleteProject {

        @Test
        @DisplayName("RUNNING 상태일 경우 삭제를 막는다")
        void deleteProject_shouldFail_whenStatusIsRunning() {
            Long userId = 1L;
            Long projectId = 100L;

            User user = User.builder().userId(userId).build();
            Project project = Project.builder().projectId(projectId).user(user).build();

            Component component = Component.builder()
                    .componentId(1L)
                    .type("SERVICE")
                    .subtype("Spring")
                    .build();

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(componentRepository.findByProject(project)).thenReturn(List.of(component));
            when(componentStatusRepository.findLatestStatus(anyString()))
                .thenReturn(Optional.of(ComponentStatusDocument.builder().status(ComponentStatus.RUNNING).build()));

            assertThatThrownBy(() -> projectService.deleteProject(userId, projectId))
                .isInstanceOf(ProjectDeletionNotAllowedException.class);
        }

        @Test
        @DisplayName("DRAFT 상태일 경우 정상 삭제된다")
        void deleteProject_success_whenStatusIsDraft() {
            Long userId = 1L;
            Long projectId = 100L;

            User user = User.builder().userId(userId).build();
            Project project = Project.builder().projectId(projectId).user(user).build();

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(componentRepository.findByProject(project)).thenReturn(List.of());
            doNothing().when(projectRepository).delete(project);

            var result = projectService.deleteProject(userId, projectId);
            assertThat(result.getMessage()).isEqualTo("프로젝트 삭제 완료");
        }
    }

    @Test
    @DisplayName("사용자의 모든 프로젝트를 DRAFT 상태로 반환한다 (초기 상태)")
    void getAllProjects_success() {
        Long userId = 1L;
        User user = User.builder().userId(userId).build();
        Project project = Project.builder().projectId(1L).user(user).projectName("P1").projectDescription("desc").createdAt(LocalDateTime.now()).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectRepository.findByUser(user)).thenReturn(List.of(project));
        when(componentRepository.findByProject(project)).thenReturn(List.of());

        List<ProjectResponseDto> result = projectService.getAllProjects(userId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ProjectStatus.DRAFT);
    }

    @Nested
    @DisplayName("getProjectById()")
    class GetProjectById {

        @Test
        @DisplayName("정상 요청 시 프로젝트 상세 정보를 반환한다")
        void getProjectById_success() {
            // given
            Long userId = 1L;
            Long projectId = 100L;

            User user = User.builder().userId(userId).build();
            Project project = Project.builder()
                    .projectId(projectId)
                    .user(user)
                    .projectName("테스트 프로젝트")
                    .projectDescription("설명")
                    .createdAt(LocalDateTime.now())
                    .build();

            Component component = Component.builder()
                    .componentId(10L)
                    .type("SERVICE")
                    .subtype("MySQL")
                    .thumbnail("mysql.png")
                    .project(project)
                    .childComponents(List.of())
                    .fromLinks(List.of())
                    .build();

            // when
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(componentRepository.findByProject(project)).thenReturn(List.of(component));
            when(componentStatusRepository.findLatestStatus("10"))
                    .thenReturn(Optional.of(ComponentStatusDocument.builder()
                            .status(ComponentStatus.RUNNING)
                            .build()));

            // then
            ProjectDetailResponseDto result = projectService.getProjectById(userId, projectId);

            assertThat(result.getProjectId()).isEqualTo(projectId);
            assertThat(result.getName()).isEqualTo("테스트 프로젝트");
            assertThat(result.getStatus()).isEqualTo(ProjectStatus.RUNNING);
            assertThat(result.getComponents()).hasSize(1);
            assertThat(result.getComponents().get(0).getStatus()).isEqualTo(ComponentStatus.RUNNING);
        }

        @Test
        @DisplayName("프로젝트가 존재하지 않으면 예외가 발생한다")
        void getProjectById_shouldFail_whenProjectNotFound() {
            when(projectRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getProjectById(1L, 100L))
                .isInstanceOf(ProjectNotFoundException.class);
        }

        @Test
        @DisplayName("다른 사용자가 접근하면 권한 예외가 발생한다")
        void getProjectById_shouldFail_whenUnauthorizedUser() {
            User projectOwner = User.builder().userId(99L).build();
            Project project = Project.builder().projectId(100L).user(projectOwner).build();

            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.getProjectById(1L, 100L))
                .isInstanceOf(UnauthorizedProjectAccessException.class);
        }
    }
}