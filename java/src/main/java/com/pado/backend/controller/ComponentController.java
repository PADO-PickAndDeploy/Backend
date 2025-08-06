package com.pado.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.pado.backend.dto.request.*;
import com.pado.backend.dto.response.*;
import com.pado.backend.service.ComponentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// TODO
@RestController
@RequiredArgsConstructor
@Tag(name = "Component", description = "컴포넌트 관리 API")
public class ComponentController {

    private final ComponentService componentService;

    @GetMapping("/components")
    @Operation(summary = "컴포넌트 종류 조회", description = "Resource 탭 클릭 시 전체 사용 가능한 컴포넌트 목록 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공적으로 컴포넌트 종류를 조회했습니다.")
    })
    public ResponseEntity<List<ComponentTypeDto>> getComponentTypes() {
        return ResponseEntity.ok(componentService.getComponentTypes());
    }

    @Operation(summary = "컴포넌트 검색")
    @GetMapping("/components/search")
    public ResponseEntity<List<ComponentSearchDto>> searchComponents(@RequestParam(name = "q") String keyword) {
        return ResponseEntity.ok(componentService.searchComponents(keyword));
    }

    @Operation(
        summary = "컴포넌트 배치",
        description = "프로젝트에 컴포넌트를 등록합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/projects/{projectId}/components")
    public ResponseEntity<ComponentDetailDto> createComponentToProject(@PathVariable Long projectId, @RequestBody ComponentCreateRequestDto request) {
        return ResponseEntity.ok(componentService.createComponentToProject(projectId, request));
    }

    @Operation(summary = "컴포넌트 설정 적용")
    @PostMapping("/projects/{projectId}/components/{componentId}/setting")
    public ResponseEntity<DefaultResponseDto> applyComponentSetting(@PathVariable Long projectId, @PathVariable Long componentId, @RequestBody ComponentSettingDto request) {
        componentService.applyComponentSetting(projectId, componentId, request);
        return ResponseEntity.ok(new DefaultResponseDto("설정 적용 완료"));
    }

    @Operation(summary = "컴포넌트 연결")
    @PostMapping("/projects/{projectId}/components/{componentId}/connect")
    public ResponseEntity<DefaultResponseDto> connectComponent(@PathVariable Long projectId, @PathVariable Long componentId, @RequestBody ComponentConnectDto request) {
        componentService.connectComponent(projectId, componentId, request);
        return ResponseEntity.ok(new DefaultResponseDto("컴포넌트 연결 완료"));
    }

    @Operation(summary = "컴포넌트 서비스 접속")
    @GetMapping("/projects/{projectId}/components/{componentId}/service")
    public ResponseEntity<ComponentServiceUrlDto> getComponentServiceUrl(@PathVariable Long projectId, @PathVariable Long componentId) {
        return ResponseEntity.ok(componentService.getComponentServiceUrl(projectId, componentId));
    }

    @Operation(summary = "배치된 컴포넌트 검색")
    @GetMapping("/projects/{projectId}/components/search")
    public ResponseEntity<List<ComponentSearchDto>> searchDeployedComponents(@PathVariable Long projectId, @RequestParam(name = "q", required = false) String keyword) {
        return ResponseEntity.ok(componentService.searchDeployedComponents(projectId, keyword));
    }

    // Socket
    @Operation(summary = "개별 컴포넌트 상태 조회")
    @GetMapping("/projects/{projectId}/components/{componentId}/status")
    public SseEmitter getComponentStatus(@PathVariable Long projectId, @PathVariable Long componentId) {
        return componentService.getComponentStatus(projectId, componentId);
    }

    // Socket
    @Operation(summary = "서비스 로그 모니터링")
    @GetMapping("/projects/{projectId}/components/{componentId}/logs")
    public SseEmitter streamComponentLogs(@PathVariable Long projectId, @PathVariable Long componentId) {
        return componentService.streamComponentLogs(projectId, componentId);
    }

    // Socket
    @Operation(summary = "모니터링")
    @GetMapping("/projects/{projectId}/components/{componentId}/monitoring")
    public SseEmitter streamMonitoring(@PathVariable Long projectId, @PathVariable Long componentId) {
        return componentService.streamMonitoring(projectId, componentId);
    }

    @Operation(summary = "특정 컴포넌트 간 연결 해제")
    @DeleteMapping("/projects/{projectId}/components/{componentId}/connect/{targetComponentId}")
    public ResponseEntity<DefaultResponseDto> disconnectSpecificComponent(
        @PathVariable Long projectId, 
        @PathVariable Long componentId,
        @PathVariable Long targetComponentId) {
        
        DefaultResponseDto response = componentService.disconnectSpecificComponent(
            projectId, componentId, targetComponentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "컴포넌트의 모든 연결 해제")
    @DeleteMapping("/projects/{projectId}/components/{componentId}/connect")
    public ResponseEntity<DefaultResponseDto> disconnect(
        @PathVariable Long projectId, 
        @PathVariable Long componentId) {
        
        DefaultResponseDto response = componentService.disconnect(projectId, componentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "컴포넌트 삭제")
    @DeleteMapping("/projects/{projectId}/components/{componentId}")
    public ResponseEntity<DefaultResponseDto> deleteComponent(@PathVariable Long projectId, @PathVariable Long componentId) {
        componentService.deleteComponent(projectId, componentId);
        return ResponseEntity.ok(new DefaultResponseDto("컴포넌트 삭제 완료"));
    }
}
