package com.pado.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pado.backend.dto.request.ProjectCreateRequestDto;
import com.pado.backend.dto.response.DefaultResponseDto;
import com.pado.backend.dto.response.ProjectDetailResponseDto;
import com.pado.backend.dto.response.ProjectResponseDto;
import com.pado.backend.global.security.userdetails.CustomUserDetails;
import com.pado.backend.service.ProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
@Tag(name = "Project", description = "프로젝트 생성/조회/삭제 API")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "프로젝트 생성")
    public ResponseEntity<ProjectResponseDto> createProject(
            @RequestBody ProjectCreateRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(projectService.createProject(request, userDetails.getUserId()));
    }

    @GetMapping
    @Operation(summary = "프로젝트 전체 조회")
    public ResponseEntity<List<ProjectResponseDto>> getAllProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(projectService.getAllProjects(userDetails.getUserId()));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "프로젝트 개별 조회")
    public ResponseEntity<ProjectDetailResponseDto> getProjectById(
        @AuthenticationPrincipal CustomUserDetails userDetails,    
        @PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProjectById(userDetails.getUserId(), projectId));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "프로젝트 개별 삭제")
    public ResponseEntity<DefaultResponseDto> deleteProject(
        @AuthenticationPrincipal CustomUserDetails userDetails,    
        @PathVariable Long projectId) {
        projectService.deleteProject(userDetails.getUserId(), projectId);
        return ResponseEntity.ok(new DefaultResponseDto("삭제 완료"));
    }
} 
