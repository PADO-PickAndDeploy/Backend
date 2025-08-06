package com.pado.backend.global.vault.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.pado.backend.global.exception.CustomException;
import com.pado.backend.global.exception.ErrorCode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Component
@ConfigurationProperties(prefix = "vault")
@Validated
@Getter
@Setter
@ToString(exclude = {"token", "kvPath"}) // 보안: 토큰과 경로 정보 로깅 방지
public class VaultProperties {
    
    /**
     * Vault 사용 여부, 기본값은 false
     */
    @NotNull
    private Boolean enabled = false; 
    
    /**
     * Vault 서버 호스트
     */
    @NotBlank(message = "Vault host cannot be blank")
    private String host;
    
    /**
     * Vault 서버 포트
     */
    @Min(value = 1, message = "Port must be greater than 0")
    @Max(value = 65535, message = "Port must be less than 65536")
    private Integer port;
    
    /**
     * Vault 서버 스키마 (http 또는 https)
     */
    @Pattern(regexp = "^(http|https)$", message = "Scheme must be 'http' or 'https'")
    private String scheme;
    
    /**
     * Vault 인증 토큰
     */
    private String token;
    
    /**
     * KV 스토어 기본 경로
     */
    @NotBlank(message = "KV path cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9/_-]+$", message = "KV path contains invalid characters")
    private String kvPath;
    
    /**
     * Vault 서버 URL 생성
     * @return 완전한 Vault 서버 URL
     */
    public String getVaultUrl() {
        return String.format("%s://%s:%d", scheme, host, port);
    }
    
    /**
     * 사용자별 Vault 경로 생성
     * @param userVaultKey 사용자 Vault 키
     * @return 사용자별 경로
     * @throws IllegalArgumentException 유효하지 않은 키인 경우
     */
    public String getUserPath(String userVaultKey) {
        validateVaultKey(userVaultKey, "User vault key");
        return String.format("%s/users/%s", kvPath, userVaultKey);
    }
    
    /**
     * 크리덴셜별 Vault 경로 생성
     * @param userVaultKey 사용자 Vault 키
     * @param credentialVaultKey 크리덴셜 Vault 키
     * @return 크리덴셜별 경로
     * @throws IllegalArgumentException 유효하지 않은 키인 경우
     */
    public String getCredentialPath(String userVaultKey, String credentialVaultKey) {
        validateVaultKey(userVaultKey, "User vault key");
        validateVaultKey(credentialVaultKey, "Credential vault key");
        return String.format("%s/users/%s/credentials/%s", kvPath, userVaultKey, credentialVaultKey);
    }
    
    /**
     * 프로젝트별 Vault 경로 생성 (확장성을 위해 추가)
     * @param userVaultKey 사용자 Vault 키
     * @param projectId 프로젝트 ID
     * @return 프로젝트별 경로
     */
    public String getProjectPath(String userVaultKey, Long projectId) {
        validateVaultKey(userVaultKey, "User vault key");
        if (projectId == null || projectId <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Project ID must be positive");
        }
        return String.format("%s/users/%s/projects/%d", kvPath, userVaultKey, projectId);
    }
    
    /**
     * 시스템 설정용 Vault 경로 생성 (확장성을 위해 추가)
     * @param configKey 설정 키
     * @return 시스템 설정 경로
     */
    public String getSystemConfigPath(String configKey) {
        if (configKey == null || configKey.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Config key cannot be null or empty");
        }
        return String.format("%s/system/config/%s", kvPath, configKey);
    }
    
    /**
     * Vault 키 유효성 검증
     * @param vaultKey 검증할 Vault 키
     * @param keyType 키 타입 (오류 메시지용)
     * @throws IllegalArgumentException 유효하지 않은 키인 경우
     */
    private void validateVaultKey(String vaultKey, String keyType) {
        if (vaultKey == null || vaultKey.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, keyType + " cannot be null or empty");
        }
        
        // 기본적인 보안 검증 (특수문자 등)
        if (!vaultKey.matches("^[a-zA-Z0-9_-]+$")) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, keyType + " contains invalid characters");
        }
        
        // 최소 길이 검증
        if (vaultKey.length() < 10) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, keyType + " must be at least 10 characters long");
        }
    }
    
    /**
     * Vault 연결 설정이 유효한지 확인
     * @return 설정 유효 여부
     */
    public boolean isValidConfiguration() {
        try {
            return enabled != null && enabled &&
                   host != null && !host.trim().isEmpty() &&
                   port != null && port > 0 && port <= 65535 &&
                   scheme != null && (scheme.equals("http") || scheme.equals("https")) &&
                   kvPath != null && !kvPath.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 개발 환경 여부 확인 (http 사용 시)
     * @return 개발 환경 여부
     */
    public boolean isDevelopmentMode() {
        return "http".equals(scheme);
    }
    
    /**
     * 운영 환경 여부 확인 (https 사용 시)
     * @return 운영 환경 여부
     */
    public boolean isProductionMode() {
        return "https".equals(scheme);
    }
}