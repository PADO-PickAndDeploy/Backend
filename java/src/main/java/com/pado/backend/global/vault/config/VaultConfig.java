package com.pado.backend.global.vault.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultToken;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class VaultConfig {
    
    private final VaultProperties vaultProperties;
    
    /**
     * VaultTemplate 빈 생성
     * vault.enabled=false인 경우 빈이 생성되지 않음
     */
    @Bean
    @ConditionalOnProperty(name = "vault.enabled", havingValue = "true", matchIfMissing = true)
    public VaultTemplate vaultTemplate() {
        try {
            // 설정 유효성 검증
            validateVaultConfiguration();
            
            // Vault 엔드포인트 생성
            VaultEndpoint vaultEndpoint = createVaultEndpoint();
            
            // 토큰 인증 설정
            TokenAuthentication authentication = createTokenAuthentication();
            
            // VaultTemplate 생성
            VaultTemplate template = new VaultTemplate(vaultEndpoint, authentication);
            
            // 보안: URL에서 민감한 정보 마스킹
            String maskedUrl = maskSensitiveUrl(vaultProperties.getVaultUrl());
            log.info("Vault template configured successfully for: {}", maskedUrl);
            
            // 연결 테스트 (선택적)
            if (vaultProperties.isDevelopmentMode()) {
                testVaultConnection(template);
            }
            
            return template;
            
        } catch (Exception e) {
            log.error("Failed to configure Vault template: {}", e.getMessage());
            throw new VaultConfigurationException("Vault configuration failed", e);
        }
    }
    
    /**
     * Vault 설정 유효성 검증
     * @throws VaultConfigurationException 설정이 유효하지 않은 경우
     */
    private void validateVaultConfiguration() {
        if (!vaultProperties.isValidConfiguration()) {
            throw new VaultConfigurationException("Invalid Vault configuration");
        }
        
        if (vaultProperties.getToken() == null || vaultProperties.getToken().trim().isEmpty()) {
            throw new VaultConfigurationException("Vault token is required");
        }
        
        // 운영환경에서 HTTP 사용 시 경고
        if (vaultProperties.isProductionMode() && "http".equals(vaultProperties.getScheme())) {
            log.warn("Using HTTP in production is not recommended. Consider using HTTPS for security.");
        }
        
        // 개발환경에서 간단한 토큰 사용 시 경고
        if (vaultProperties.isDevelopmentMode() && isWeakToken(vaultProperties.getToken())) {
            log.warn("Using a weak token in development. Consider using a stronger token for better security practices.");
        }
    }
    
    /**
     * Vault 엔드포인트 생성
     * @return VaultEndpoint 객체
     * @throws VaultConfigurationException URI 생성 실패 시
     */
    private VaultEndpoint createVaultEndpoint() {
        try {
            URI vaultUri = new URI(vaultProperties.getVaultUrl());
            return VaultEndpoint.from(vaultUri);
        } catch (URISyntaxException e) {
            throw new VaultConfigurationException("Invalid Vault URL: " + vaultProperties.getVaultUrl(), e);
        }
    }
    
    /**
     * 토큰 인증 객체 생성
     * @return TokenAuthentication 객체
     */
    private TokenAuthentication createTokenAuthentication() {
        VaultToken token = VaultToken.of(vaultProperties.getToken());
        return new TokenAuthentication(token);
    }
    
    /**
     * Vault 연결 테스트 (개발환경에서만)
     * @param template VaultTemplate 객체
     */
    private void testVaultConnection(VaultTemplate template) {
        try {
            template.opsForSys().health();
            log.debug("Vault connection test successful");
        } catch (Exception e) {
            log.warn("Vault connection test failed: {}. Application will continue but Vault operations may fail.", e.getMessage());
        }
    }
    
    /**
     * URL에서 민감한 정보 마스킹
     * @param url 원본 URL
     * @return 마스킹된 URL
     */
    private String maskSensitiveUrl(String url) {
        if (url == null) {
            return "***";
        }
        
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            
            // 호스트 일부 마스킹 (localhost는 예외)
            String maskedHost = "localhost".equals(host) ? host : maskHost(host);
            
            return String.format("%s://%s:%d", scheme, maskedHost, port);
        } catch (URISyntaxException e) {
            return "***";
        }
    }
    
    /**
     * 호스트 정보 마스킹
     * @param host 원본 호스트
     * @return 마스킹된 호스트
     */
    private String maskHost(String host) {
        if (host == null || host.length() <= 4) {
            return "***";
        }
        
        return host.substring(0, 2) + "***" + host.substring(host.length() - 2);
    }
    
    /**
     * 약한 토큰인지 확인
     * @param token 토큰 문자열
     * @return 약한 토큰 여부
     */
    private boolean isWeakToken(String token) {
        return token != null && (
            token.length() < 10 ||
            token.matches("^[0-9]+$") || // 숫자만
            token.matches("^[a-z]+$") || // 소문자만
            token.contains("test") ||
            token.contains("dev") ||
            token.contains("123")
        );
    }
    
    /**
     * Vault가 비활성화된 경우를 위한 더미 VaultTemplate
     * 실제로는 사용되지 않지만 의존성 주입 오류 방지
     */
    @Bean
    @ConditionalOnProperty(name = "vault.enabled", havingValue = "false")
    public VaultTemplate disabledVaultTemplate() {
        log.info("Vault is disabled. Creating disabled VaultTemplate bean.");
        return null; // VaultService에서 null 체크로 처리
    }
    
    /**
     * Vault 설정 예외 클래스
     */
    public static class VaultConfigurationException extends RuntimeException {
        public VaultConfigurationException(String message) {
            super(message);
        }
        
        public VaultConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}