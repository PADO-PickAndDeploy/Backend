package com.pado.backend.global.vault.service;

import com.pado.backend.domain.Credential;
import com.pado.backend.domain.User;
import com.pado.backend.global.vault.config.VaultProperties;
import com.pado.backend.global.vault.util.VaultKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialVaultService {
    
    private final VaultService vaultService;
    private final VaultProperties vaultProperties;
    
    /**
     * 사용자 크리덴셜을 Vault에 저장
     * @param user 사용자
     * @param credential 크리덴셜
     * @param credentialData 실제 크리덴셜 데이터 (JSON 문자열)
     */
    public void storeCredential(User user, Credential credential, String credentialData) {
        // User와 Credential에 vaultKey가 없으면 생성
        if (user.getVaultKey() == null || user.getVaultKey().isEmpty()) {
            throw new IllegalArgumentException("User must have a vault key");
        }
        
        if (credential.getVaultKey() == null || credential.getVaultKey().isEmpty()) {
            throw new IllegalArgumentException("Credential must have a vault key");
        }
        
        String path = vaultProperties.getCredentialPath(user.getVaultKey(), credential.getVaultKey());
        
        Map<String, Object> vaultData = new HashMap<>();
        vaultData.put("credentialId", credential.getCredentialId());
        vaultData.put("credentialName", credential.getCredentialName());
        vaultData.put("credentialType", credential.getCredentialType());
        vaultData.put("credentialDescription", credential.getCredentialDescription());
        vaultData.put("credentialData", credentialData); // 실제 민감한 데이터
        vaultData.put("userId", user.getUserId());
        vaultData.put("createdAt", credential.getCreatedAt().toString());
        
        vaultService.writeSecret(path, vaultData);
        log.info("Credential stored in Vault for user: {} at path: {}", user.getUserId(), path);
    }
    
    /**
     * Vault에서 크리덴셜 데이터 조회
     * @param user 사용자
     * @param credential 크리덴셜
     * @return 크리덴셜 데이터 (JSON 문자열)
     */
    public String getCredentialData(User user, Credential credential) {
        String path = vaultProperties.getCredentialPath(user.getVaultKey(), credential.getVaultKey());
        
        Map<String, Object> vaultData = vaultService.readSecret(path);
        if (vaultData == null) {
            log.warn("Credential data not found in Vault for path: {}", path);
            return null;
        }
        
        return (String) vaultData.get("credentialData");
    }
    
    /**
     * Vault에서 전체 크리덴셜 정보 조회 (디버깅용)
     * @param user 사용자
     * @param credential 크리덴셜
     * @return 전체 크리덴셜 정보
     */
    public Map<String, Object> getFullCredentialInfo(User user, Credential credential) {
        String path = vaultProperties.getCredentialPath(user.getVaultKey(), credential.getVaultKey());
        return vaultService.readSecret(path);
    }
    
    /**
     * Vault에서 크리덴셜 삭제
     * @param user 사용자
     * @param credential 크리덴셜
     */
    public void deleteCredential(User user, Credential credential) {
        String path = vaultProperties.getCredentialPath(user.getVaultKey(), credential.getVaultKey());
        vaultService.deleteSecret(path);
        log.info("Credential deleted from Vault for user: {} at path: {}", user.getUserId(), path);
    }
    
    /**
     * 크리덴셜 데이터 업데이트
     * @param user 사용자
     * @param credential 크리덴셜
     * @param newCredentialData 새로운 크리덴셜 데이터
     */
    public void updateCredential(User user, Credential credential, String newCredentialData) {
        // 기존 데이터 조회
        Map<String, Object> existingData = getFullCredentialInfo(user, credential);
        if (existingData == null) {
            // 데이터가 없으면 새로 생성
            storeCredential(user, credential, newCredentialData);
            return;
        }
        
        // credentialData만 업데이트
        existingData.put("credentialData", newCredentialData);
        existingData.put("updatedAt", credential.getUpdatedAt().toString());
        
        String path = vaultProperties.getCredentialPath(user.getVaultKey(), credential.getVaultKey());
        vaultService.writeSecret(path, existingData);
        log.info("Credential updated in Vault for user: {} at path: {}", user.getUserId(), path);
    }
    
    /**
     * 새 사용자를 위한 Vault 키 생성 및 설정
     * @return 생성된 Vault 키
     */
    public String generateUserVaultKey() {
        return VaultKeyUtil.generateVaultKey();
    }
    
    /**
     * 새 크리덴셜을 위한 Vault 키 생성 및 설정
     * @return 생성된 Vault 키
     */
    public String generateCredentialVaultKey() {
        return VaultKeyUtil.generateVaultKey();
    }
}