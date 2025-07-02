package com.pado.backend.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * TODO : 사용자 역할을 정의하는 Enum, 추후 수정이 필요할 수도 있음.
 */
public enum Role {
    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자");
    
    private final String authority;
    private final String description;
    
    Role(String authority, String description) {
        this.authority = authority;
        this.description = description;
    }
    
    /**
     * Spring Security GrantedAuthority로 변환
     * @return GrantedAuthority 객체
     */
    public GrantedAuthority toGrantedAuthority() {
        return new SimpleGrantedAuthority(this.authority);
    }
    
    /**
     * ROLE_ 접두사가 포함된 권한 문자열 반환
     * @return 권한 문자열 (예: "ROLE_USER")
     */
    public String getAuthority() {
        return this.authority;
    }
    
    /**
     * 역할 설명 반환
     * @return 설명
     */
    public String getDescription() {
        return this.description;
    }
    
    /**
     * JSON 직렬화 시 사용할 값
     * @return enum name (예: "USER")
     */
    @JsonValue
    public String getValue() {
        return this.name();
    }
    
    /**
     * 문자열을 Role enum으로 변환 (JSON 역직렬화 시 사용)
     * @param value 역할 문자열
     * @return Role enum
     * @throws IllegalArgumentException 유효하지 않은 역할일 때
     */
    @JsonCreator
    public static Role from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Role 값이 null이거나 비어있습니다");
        }
        
        // 다양한 형태의 입력 처리
        String cleanValue = value.trim()
                                .replace("ROLE_", "")  // "ROLE_USER" → "USER"
                                .toUpperCase();        // "user" → "USER"
        
        try {
            return Role.valueOf(cleanValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 Role: " + value + " (사용 가능: USER, ADMIN)");
        }
    }
    
    /**
     * 문자열을 Role enum으로 변환 (기본값 제공)
     * @param value 역할 문자열
     * @param defaultRole 변환 실패 시 반환할 기본값
     * @return Role enum
     */
    public static Role fromWithDefault(String value, Role defaultRole) {
        try {
            return from(value);
        } catch (IllegalArgumentException e) {
            return defaultRole;
        }
    }
    
    /**
     * 관리자 권한 여부 확인
     * @return 관리자면 true
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    /**
     * 일반 사용자 여부 확인
     * @return 일반 사용자면 true
     */
    public boolean isUser() {
        return this == USER;
    }
}