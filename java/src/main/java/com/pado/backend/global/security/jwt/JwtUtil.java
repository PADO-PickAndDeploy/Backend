package com.pado.backend.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:3600000}")  // 기본 1시간 (1000 * 60 * 60)
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")  // 기본 7일 (1000 * 60 * 60 * 24 * 7)
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        // secret이 너무 짧으면 에러가 발생할 수 있으므로 체크
        if (secret.length() < 32) {
            log.warn("JWT secret key가 너무 짧습니다. 최소 32자 이상 권장");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 생성
     * @param userId 사용자 ID
     * @return JWT Access Token
     */
    public String generateAccessToken(String userId) {
        return generateToken(userId, accessTokenExpiration);
    }

    /**
     * Refresh Token 생성
     * @param userId 사용자 ID
     * @return JWT Refresh Token
     */
    public String generateRefreshToken(String userId) {
        return generateToken(userId, refreshTokenExpiration);
    }

    /**
     * JWT 토큰 생성 (공통 로직)
     */
    private String generateToken(String userId, long expirationTimeMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTimeMs);

        return Jwts.builder()
                .setSubject(userId)
                .claim("type", expirationTimeMs == accessTokenExpiration ? "ACCESS" : "REFRESH")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * JWT 토큰 유효성 검증
     * @param token JWT 토큰
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
            return false;
        } catch (SecurityException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            return false;
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Long extractUserId(String token) {
        String userIdStr = extractClaims(token).getSubject();
        return Long.parseLong(userIdStr);
    }

    /**
     * JWT 토큰에서 만료시간 추출
     * @param token JWT 토큰
     * @return 만료시간
     */
    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    /**
     * JWT 토큰이 만료되었는지 확인
     * @param token JWT 토큰
     * @return 만료되었으면 true
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * JWT 토큰에서 Claims 추출 (공통 로직)
     * @param token JWT 토큰
     * @return Claims
     */
    private Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 Claims는 읽을 수 있음 (리프레시할 때 필요)
            return e.getClaims();
        }
    }

    /**
     * Access Token인지 확인
     * @param token JWT 토큰
     * @return Access Token이면 true
     */
    public boolean isAccessToken(String token) {
        try {
            String type = extractClaims(token).get("type", String.class);
            return "ACCESS".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Refresh Token인지 확인
     * @param token JWT 토큰
     * @return Refresh Token이면 true
     */
    public boolean isRefreshToken(String token) {
        try {
            String type = extractClaims(token).get("type", String.class);
            return "REFRESH".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

}