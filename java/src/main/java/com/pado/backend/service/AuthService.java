package com.pado.backend.service;

import org.springframework.security.core.Authentication;
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
import com.pado.backend.global.security.userdetails.CustomUserDetails;
import com.pado.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
// TODO: 서비스 로직 구현
public class AuthService {
    private final UserRepository userRepository;

    public UserRegisterResponseDto signup(UserRegisterRequestDto request) {
        return null;
    }

    public UserLoginResponseDto signin(UserLoginRequestDto request) {
        return null;
    }

    // 기존 userId를 받아서 백엔드에서 본인 계정인지 확인한 뒤 로그아웃
    // public DefaultResponseDto signout(UserLogoutRequestDto request) {
    //    return null;
    // }
    
    // JWT 토큰만 받아서 로그아웃
    @Transactional
    public DefaultResponseDto signout(Authentication authentication) {
        try {
            // JWT 토큰에서 현재 로그인한 사용자 정보 추출
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            Long userId = userDetails.getUserId();
            String userEmail = userDetails.getEmail();
            String userName = userDetails.getName();
            Role userRole = userDetails.getRole();
            
            // 로그아웃 로그 기록
            log.info("사용자 로그아웃 - ID: {}, Email: {}, Name: {}, Role: {}", 
                    userId, userEmail, userName, userRole);
                
            // 로그아웃 성공 응답
            return new DefaultResponseDto("로그아웃이 완료되었습니다");
            
        } catch (ClassCastException e) {
            // Authentication 객체에서 CustomUserDetails 추출 실패
            log.error("로그아웃 처리 중 사용자 정보 추출 실패: {}", e.getMessage());
            throw new InvalidAuthenticationException();
            
        } catch (Exception e) {
            // 기타 예상치 못한 오류
            log.error("로그아웃 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            
            // 보안상 이유로 로그아웃은 항상 성공으로 응답
            // (공격자에게 시스템 정보 노출 방지)
            return new DefaultResponseDto("로그아웃이 완료되었습니다");
        }
    }
}
