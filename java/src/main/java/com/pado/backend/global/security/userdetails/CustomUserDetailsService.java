package com.pado.backend.global.security.userdetails;

import com.pado.backend.domain.User;
import com.pado.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService 인터페이스 구현체
 * 사용자 인증 시 DB에서 사용자 정보를 조회하는 역할
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * 사용자명(이메일)으로 사용자 정보를 조회
     * Spring Security가 인증 시 자동으로 호출하는 메소드
     * 
     * @param email 사용자 이메일
     * @return UserDetails 구현체 (CustomUserDetails)
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("사용자 인증 시도: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음: {}", email);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
                });
        
        log.debug("사용자 조회 성공: {} (역할: {})", user.getEmail(), user.getRole());
        return new CustomUserDetails(user);
    }
    
    /**
     * 사용자 ID로 사용자 정보를 조회 (JWT 인증용)
     * 
     * @param userId 사용자 ID
     * @return CustomUserDetails
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때
     */
    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("사용자 ID로 조회: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자 ID를 찾을 수 없음: {}", userId);
                    return new UsernameNotFoundException("사용자 ID를 찾을 수 없습니다: " + userId);
                });
        
        log.debug("사용자 ID 조회 성공: {} (이메일: {})", userId, user.getEmail());
        return new CustomUserDetails(user);
    }
    
    /**
     * 이메일로 사용자 정보를 조회 (명시적 메소드)
     * 
     * @param email 사용자 이메일
     * @return CustomUserDetails
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때
     */
    @Transactional(readOnly = true)
    public CustomUserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        return (CustomUserDetails) loadUserByUsername(email);
    }
}