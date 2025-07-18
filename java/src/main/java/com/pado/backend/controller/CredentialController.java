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

import com.pado.backend.dto.request.CredentialCreateRequestDto;
import com.pado.backend.dto.response.CredentialDetailResponseDto;
import com.pado.backend.dto.response.CredentialResponseDto;
import com.pado.backend.dto.response.DefaultResponseDto;
import com.pado.backend.global.security.userdetails.CustomUserDetails;
import com.pado.backend.service.CredentialService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/credentials")
@RequiredArgsConstructor
@Tag(name = "Credential", description = "자격 증명 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class CredentialController {

    private final CredentialService credentialService;

    @Operation(summary = "Credential 관리 (등록)", description = "특정 유저에 대해 자격 증명을 등록합니다.")
    @PostMapping
    public ResponseEntity<CredentialResponseDto> createCredential(
            @RequestBody CredentialCreateRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(credentialService.createCredential(request, userDetails.getUserId()));
    }

    @Operation(summary = "Credential 관리 (전체 조회)", description = "특정 유저가 등록한 모든 자격 증명을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<CredentialResponseDto>> getAllCredentials(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(credentialService.getAllCredentials(userDetails.getUserId()));
    }

    @Operation(summary = "Credential 관리 (개별 조회)", description = "특정 유저의 자격 증명 하나를 상세 조회합니다.")
    @GetMapping("/{credentialId}")
    public ResponseEntity<CredentialDetailResponseDto> getCredential(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long credentialId) {
        return ResponseEntity.ok(credentialService.getCredential(userDetails.getUserId(), credentialId));
    }

    @Operation(summary = "Credential 관리 (삭제)", description = "특정 유저의 자격 증명을 삭제합니다.")
    @DeleteMapping("/{credentialId}")
    public ResponseEntity<DefaultResponseDto> deleteCredential(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long credentialId) {
        credentialService.deleteCredential(userDetails.getUserId(), credentialId);
        return ResponseEntity.ok(new DefaultResponseDto("크리덴셜 삭제 완료"));
    }
}