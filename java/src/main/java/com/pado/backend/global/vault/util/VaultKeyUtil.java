package com.pado.backend.global.vault.util;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class VaultKeyUtil {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    
    // 키 타입별 접두사
    private static final String USER_PREFIX = "USR_";
    private static final String CREDENTIAL_PREFIX = "CRD_";
    
    /**
     * 사용자용 Vault 키 생성
     * @return 생성된 사용자 Vault 키
     */
    public static String generateUserVaultKey() {
        String key = USER_PREFIX + generateSecureKey();
        log.debug("Generated user vault key with prefix: {}", USER_PREFIX);
        return key;
    }
    
    /**
     * 크리덴셜용 Vault 키 생성
     * @return 생성된 크리덴셜 Vault 키
     */
    public static String generateCredentialVaultKey() {
        String key = CREDENTIAL_PREFIX + generateSecureKey();
        log.debug("Generated credential vault key with prefix: {}", CREDENTIAL_PREFIX);
        return key;
    }
    
    /**
     * 일반 Vault 키 생성 (하위 호환성)
     * @return 생성된 Vault 키
     */
    public static String generateVaultKey() {
        return generateSecureKey();
    }
    
    /**
     * 보안 강화된 키 생성 (내부 메서드)
     * @return 생성된 보안 키
     */
    private static String generateSecureKey() {
        // UUID + 타임스탬프 + 랜덤 문자열 조합
        String uuid = UUID.randomUUID().toString().replace("-", "");
        long timestamp = Instant.now().toEpochMilli();
        String randomSuffix = generateRandomString(8);
        
        return uuid + timestamp + randomSuffix;
    }
    
    /**
     * 지정된 길이의 랜덤 문자열 생성
     * @param length 생성할 문자열 길이
     * @return 랜덤 문자열
     */
    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(SECURE_RANDOM.nextInt(CHARS.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * 사용자 Vault 키 유효성 검증
     * @param vaultKey 검증할 키
     * @return 유효하면 true
     */
    public static boolean isValidUserVaultKey(String vaultKey) {
        return vaultKey != null && 
               vaultKey.startsWith(USER_PREFIX) &&
               vaultKey.length() >= (USER_PREFIX.length() + 40) && // 최소 길이 체크
               vaultKey.substring(USER_PREFIX.length()).matches("^[a-zA-Z0-9]+$"); // 접두사 이후 영숫자만 허용
    }
    
    /**
     * 크리덴셜 Vault 키 유효성 검증
     * @param vaultKey 검증할 키
     * @return 유효하면 true
     */
    public static boolean isValidCredentialVaultKey(String vaultKey) {
        return vaultKey != null && 
               vaultKey.startsWith(CREDENTIAL_PREFIX) &&
               vaultKey.length() >= (CREDENTIAL_PREFIX.length() + 40) && // 최소 길이 체크
               vaultKey.substring(CREDENTIAL_PREFIX.length()).matches("^[a-zA-Z0-9]+$"); // 접두사 이후 영숫자만 허용
    }
    
    /**
     * 일반 Vault 키 유효성 검증 (하위 호환성)
     * @param vaultKey 검증할 키
     * @return 유효하면 true
     */
    public static boolean isValidVaultKey(String vaultKey) {
        return vaultKey != null && 
               vaultKey.length() >= 40 && // 최소 길이 체크
               vaultKey.matches("^[a-zA-Z0-9_]+$"); // 영숫자와 언더스코어 허용
    }
    
    /**
     * 키 타입 확인
     * @param vaultKey 확인할 키
     * @return 키 타입 (USER, CREDENTIAL, UNKNOWN)
     */
    public static KeyType getKeyType(String vaultKey) {
        if (vaultKey == null) {
            return KeyType.UNKNOWN;
        }
        
        if (vaultKey.startsWith(USER_PREFIX)) {
            return KeyType.USER;
        } else if (vaultKey.startsWith(CREDENTIAL_PREFIX)) {
            return KeyType.CREDENTIAL;
        } else {
            return KeyType.UNKNOWN;
        }
    }
    
    /**
     * Vault 키 타입 열거형
     */
    public enum KeyType {
        USER, CREDENTIAL, UNKNOWN
    }
}