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
     * Spring Security 인증용 - userName 기반
     * @param userName 사용자명 (로그인 ID)
     * @return UserDetails 구현체
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        log.debug("사용자 인증 시도: {}", userName);
        
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음: {}", userName);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userName);
                });
        
        log.debug("사용자 조회 성공: {} (ID: {})", user.getUserName(), user.getUserId());
        return new CustomUserDetails(user);
    }
    
    /**
     * JWT 인증용 - userId 기반
     */
    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("사용자 ID로 조회: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자 ID를 찾을 수 없음: {}", userId);
                    return new UsernameNotFoundException("사용자 ID를 찾을 수 없습니다: " + userId);
                });
        
        log.debug("사용자 ID 조회 성공: {} (사용자명: {})", userId, user.getUserName());
        return new CustomUserDetails(user);
    }
}