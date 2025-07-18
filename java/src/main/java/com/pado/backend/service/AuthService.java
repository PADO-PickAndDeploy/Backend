package com.pado.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pado.backend.domain.Role;
import com.pado.backend.domain.User;
import com.pado.backend.dto.request.UserLoginRequestDto;
import com.pado.backend.dto.request.UserLogoutRequestDto;
import com.pado.backend.dto.request.UserRegisterRequestDto;
import com.pado.backend.dto.response.DefaultResponseDto;
import com.pado.backend.dto.response.UserLoginResponseDto;
import com.pado.backend.dto.response.UserRegisterResponseDto;
import com.pado.backend.global.exception.InvalidAuthenticationException;
import com.pado.backend.global.security.jwt.JwtUtil;
import com.pado.backend.global.security.jwt.TokenBlacklistService;
import com.pado.backend.global.security.userdetails.CustomUserDetails;
import com.pado.backend.global.vault.util.VaultKeyUtil;
import com.pado.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 회원가입
     * @param request 회원가입 요청 DTO
     * @return 회원가입 응답 DTO (JWT 토큰 포함)
     */
    @Transactional
    public UserRegisterResponseDto signup(UserRegisterRequestDto request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        User newUser = User.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .vaultKey(VaultKeyUtil.generateVaultKey())
                .build();

        User savedUser = userRepository.save(newUser);

        // JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(savedUser.getUserId().toString());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getUserId().toString());
        
        return new UserRegisterResponseDto(
                savedUser.getUserId(),
                "회원 가입이 성공적으로 완료되었습니다.",
                accessToken,
                refreshToken
        );
    }

    /**
     * 로그인 - 보안 로깅 적용
     * @param request 로그인 요청 DTO
     * @return 로그인 응답 DTO (JWT 토큰 포함)
     */
    @Transactional(readOnly = true)
    public UserLoginResponseDto signin(UserLoginRequestDto request) {
        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(InvalidLoginCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidLoginCredentialsException();
        }

        // Spring Security 인증 처리 (아이디 기반)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUserName(), request.getPassword())
        );
        
        // 사용자 ID로 JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user.getUserId().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId().toString());
        
        return new UserLoginResponseDto(accessToken, refreshToken);
    }
    
    /**
     * 로그아웃 (블랙리스트 방식)
     * @param userDetails 현재 인증된 사용자 정보
     * @param accessToken 무효화할 Access Token
     * @return 로그아웃 응답 DTO
     */
    @Transactional
    public DefaultResponseDto signout(CustomUserDetails userDetails, String accessToken) {
        try {
            log.info("로그아웃 요청: 사용자ID={}", userDetails.getUserId());
            
            // Access Token을 블랙리스트에 추가
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                tokenBlacklistService.blacklistToken(accessToken);
            }
            
            log.info("로그아웃 완료: 사용자ID={}", userDetails.getUserId());
            return new DefaultResponseDto("로그아웃이 완료되었습니다");
            
        } catch (Exception e) {
            log.error("로그아웃 중 예외 발생: {}", e.getMessage());
            return new DefaultResponseDto("로그아웃이 완료되었습니다");
        }
    }

}
