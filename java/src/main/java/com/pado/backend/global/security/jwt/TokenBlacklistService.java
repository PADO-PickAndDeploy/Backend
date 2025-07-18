package com.pado.backend.global.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;
    
    private static final String BLACKLIST_PREFIX = "blacklist:";
    
    /**
     * 토큰을 블랙리스트에 추가
     * @param token JWT 토큰
     */
    public void blacklistToken(String token) {
        try {
            // 토큰의 만료 시간 계산
            Date expirationDate = jwtUtil.extractExpiration(token);
            long now = System.currentTimeMillis();
            long expiration = expirationDate.getTime() - now;
            
            // 이미 만료된 토큰은 블랙리스트에 추가하지 않음
            if (expiration <= 0) {
                log.debug("이미 만료된 토큰이므로 블랙리스트에 추가하지 않음");
                return;
            }
            
            // Redis에 토큰을 블랙리스트로 등록 (만료 시간까지만)
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "true", Duration.ofMillis(expiration));
            
            log.info("토큰이 블랙리스트에 추가됨: 만료까지 {}ms", expiration);
            
        } catch (Exception e) {
            log.error("토큰 블랙리스트 추가 중 오류 발생: {}", e.getMessage());
            // 블랙리스트 실패해도 로그아웃은 성공으로 처리 (보안상)
        }
    }
    
    /**
     * 토큰이 블랙리스트에 있는지 확인
     * @param token JWT 토큰
     * @return 블랙리스트에 있으면 true
     */
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("블랙리스트 확인 중 오류 발생: {}", e.getMessage());
            // Redis 오류 시 안전을 위해 블랙리스트로 간주
            return true;
        }
    }
    
}
