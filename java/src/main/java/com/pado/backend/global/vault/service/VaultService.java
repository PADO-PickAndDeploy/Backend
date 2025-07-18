package com.pado.backend.global.vault.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pado.backend.global.vault.config.VaultProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VaultService {
    
    private final VaultTemplate vaultTemplate;
    private final VaultProperties vaultProperties;
    private final ObjectMapper objectMapper;
    
    /**
     * Vault에 데이터 저장
     * @param path Vault 경로
     * @param data 저장할 데이터
     */
    public void writeSecret(String path, Map<String, Object> data) {
        if (!vaultProperties.getEnabled() || vaultTemplate == null) {
            log.warn("Vault is disabled. Secret will not be stored.");
            return;
        }
        
        try {
            vaultTemplate.write(path, data);
            // 보안: 경로 정보를 마스킹하여 로깅
            log.debug("Secret written successfully to path: {}", maskSensitivePath(path));
        } catch (Exception e) {
            // 보안: 경로 정보 노출 최소화
            log.error("Failed to write secret to Vault: {}", e.getMessage());
            throw new VaultOperationException("Vault write operation failed", e);
        }
    }
    
    /**
     * Vault에서 데이터 읽기
     * @param path Vault 경로
     * @return 저장된 데이터
     */
    public Map<String, Object> readSecret(String path) {
        if (!vaultProperties.getEnabled() || vaultTemplate == null) {
            log.warn("Vault is disabled. Returning null for secret read.");
            return null;
        }
        
        try {
            VaultResponse response = vaultTemplate.read(path);
            if (response != null && response.getData() != null) {
                log.debug("Secret read successfully from path: {}", maskSensitivePath(path));
                return response.getData();
            }
            log.debug("No secret found at path: {}", maskSensitivePath(path));
            return null;
        } catch (Exception e) {
            log.error("Failed to read secret from Vault: {}", e.getMessage());
            throw new VaultOperationException("Vault read operation failed", e);
        }
    }
    
    /**
     * Vault에서 데이터 삭제
     * @param path Vault 경로
     */
    public void deleteSecret(String path) {
        if (!vaultProperties.getEnabled() || vaultTemplate == null) {
            log.warn("Vault is disabled. Secret will not be deleted.");
            return;
        }
        
        try {
            vaultTemplate.delete(path);
            log.debug("Secret deleted successfully from path: {}", maskSensitivePath(path));
        } catch (Exception e) {
            log.error("Failed to delete secret from Vault: {}", e.getMessage());
            throw new VaultOperationException("Vault delete operation failed", e);
        }
    }
    
    /**
     * Vault 연결 상태 확인
     * @return Vault 사용 가능 여부
     */
    public boolean isVaultAvailable() {
        if (!vaultProperties.getEnabled() || vaultTemplate == null) {
            return false;
        }
        
        try {
            // 간단한 health check (KV v2 엔진 확인)
            vaultTemplate.opsForSys().health();
            return true;
        } catch (Exception e) {
            log.warn("Vault health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * JSON 문자열을 Map으로 변환 (타입 안전성 개선)
     * @param jsonString JSON 문자열
     * @return Map 객체
     */
    public Map<String, Object> jsonToMap(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        
        try {
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            // 보안: JSON 내용을 로그에 출력하지 않음
            log.error("Failed to convert JSON to Map: {}", e.getMessage());
            throw new VaultOperationException("JSON parsing failed", e);
        }
    }
    
    /**
     * Map을 JSON 문자열로 변환
     * @param data Map 객체
     * @return JSON 문자열
     */
    public String mapToJson(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            // 보안: Map 내용을 로그에 출력하지 않음
            log.error("Failed to convert Map to JSON: {}", e.getMessage());
            throw new VaultOperationException("JSON serialization failed", e);
        }
    }
    
    /**
     * 민감한 경로 정보를 마스킹
     * @param path 원본 경로
     * @return 마스킹된 경로
     */
    private String maskSensitivePath(String path) {
        if (path == null || path.length() <= 10) {
            return "***";
        }
        
        // 경로의 시작과 끝만 보여주고 중간은 마스킹
        String prefix = path.substring(0, Math.min(10, path.length()));
        String suffix = path.length() > 20 ? path.substring(path.length() - 5) : "";
        
        return prefix + "***" + suffix;
    }
    
    /**
     * Vault 전용 예외 클래스
     */
    public static class VaultOperationException extends RuntimeException {
        public VaultOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}