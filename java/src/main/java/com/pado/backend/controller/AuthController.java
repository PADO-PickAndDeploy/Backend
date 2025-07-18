package com.pado.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pado.backend.dto.request.UserLoginRequestDto;
import com.pado.backend.dto.request.UserLogoutRequestDto;
import com.pado.backend.dto.request.UserRegisterRequestDto;
import com.pado.backend.dto.response.DefaultResponseDto;
import com.pado.backend.dto.response.UserLoginResponseDto;
import com.pado.backend.dto.response.UserRegisterResponseDto;
import com.pado.backend.global.security.userdetails.CustomUserDetails;
import com.pado.backend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원가입", description = "유저 정보를 기반으로 회원가입을 진행합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "회원가입 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserRegisterResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (유효성 검증 실패, 중복된 이메일 등)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "이미 존재하는 사용자",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        )
    })
    @PostMapping("/signup")
    public ResponseEntity<UserRegisterResponseDto> signup(@Valid @RequestBody UserRegisterRequestDto request) {
        return ResponseEntity.ok(authService.signup(request));
    }
    
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인을 시도합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserLoginResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (필수 필드 누락 등)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패 (잘못된 이메일 또는 비밀번호)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 사용자",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        )
    })
    @PostMapping("/signin")
    public ResponseEntity<UserLoginResponseDto> signin(@Valid @RequestBody UserLoginRequestDto request) {
        return ResponseEntity.ok(authService.signin(request));
    }

    @Operation(summary = "로그아웃", description = "로그아웃 처리를 수행합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (유효하지 않은 토큰 등)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증되지 않은 사용자 (토큰 만료 또는 무효)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DefaultResponseDto.class)
            )
        )
    })
    @PostMapping("/signout")
    public ResponseEntity<DefaultResponseDto> signout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        
        // Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        
        return ResponseEntity.ok(authService.signout(userDetails, accessToken));
    }
}
