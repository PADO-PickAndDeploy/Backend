package com.pado.backend.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 프로젝트 전체에서 사용되는 에러 코드를 관리하는 Enum 클래스
 * 각 에러 코드는 고유한 코드, 메시지, HTTP 상태 코드를 가진다.
 * TODO : Spring에서 자동으로 처리하는 시스템 관련 에러는 GlobalExceptionHandler로 이관 가능함
 */
public enum ErrorCode {
    
    // ===== 공통 에러 =====
    INTERNAL_SERVER_ERROR("C001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("C002", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("C003", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("C004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("C005", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    
    // ===== JWT 인증 관련 에러 =====
    JWT_TOKEN_EXPIRED("J001", "토큰이 만료되었습니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED),
    JWT_TOKEN_INVALID("J002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    JWT_TOKEN_MALFORMED("J003", "토큰 형식이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    JWT_TOKEN_SIGNATURE_INVALID("J004", "토큰 서명이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    JWT_AUTHENTICATION_REQUIRED("J005", "인증이 필요합니다. 로그인 후 다시 시도해주세요.", HttpStatus.UNAUTHORIZED),
    
    // ===== 사용자 관련 에러 =====
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("U002", "이미 존재하는 사용자입니다.", HttpStatus.CONFLICT),
    USER_AUTHENTICATION_FAILED("U003", "사용자 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    
    // ===== 크리덴셜 관련 에러 =====
    CREDENTIAL_NOT_FOUND("CR001", "크리덴셜을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CREDENTIAL_ACCESS_DENIED("CR002", "크리덴셜에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // ===== 프로젝트 관련 에러 =====
    PROJECT_NOT_FOUND("P001", "프로젝트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PROJECT_ACCESS_DENIED("P002", "프로젝트에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    PROJECT_ALREADY_EXISTS("P003", "이미 존재하는 프로젝트입니다.", HttpStatus.CONFLICT),
    PROJECT_DELETION_NOT_ALLOWED("P004", "RUNNING 상태의 프로젝트는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // ===== 컴포넌트 관련 에러 =====
    COMPONENT_PROJECT_MISMATCH("CP001", "해당 컴포넌트는 요청한 프로젝트에 속해 있지 않습니다.", HttpStatus.BAD_REQUEST),
    COMPONENT_DELETION_NOT_ALLOWED("CP002", "RUNNING 상태의 컴포넌트는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_COMPONENT_REQUEST("CP003", "SERVICE 타입은 반드시 부모 컴포넌트가 필요합니다.", HttpStatus.BAD_REQUEST),
    COMPONENT_NOT_FOUND("CP004", "컴포넌트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMPONENT_STATE_INVALID("CP005", "컴포넌트 상태가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),

    // ===== Vault 관련 에러 =====
    VAULT_OPERATION_FAILED("V001", "Vault 작업이 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    VAULT_CONNECTION_FAILED("V002", "Vault 연결에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    VAULT_AUTHENTICATION_FAILED("V003", "Vault 인증에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    VAULT_CONFIGURATION_ERROR("V004", "Vault 설정 오류입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    VAULT_SECRET_NOT_FOUND("V005", "Vault에서 시크릿을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    
    // ===== 데이터 관련 에러 =====
    JSON_PARSING_ERROR("D001", "JSON 파싱 중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST),
    DATABASE_CONNECTION_ERROR("D002", "데이터베이스 연결 오류입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    DATA_INTEGRITY_VIOLATION("D003", "데이터 무결성 제약 조건을 위반했습니다.", HttpStatus.BAD_REQUEST),
    
    // ===== 파일 관련 에러 =====
    FILE_NOT_FOUND("F001", "파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED("F002", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_SIZE_EXCEEDED("F003", "파일 크기가 제한을 초과했습니다.", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_SUPPORTED("F004", "지원하지 않는 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    
    // ===== 네트워크 관련 에러 =====
    NETWORK_TIMEOUT("N001", "네트워크 요청이 시간 초과되었습니다.", HttpStatus.REQUEST_TIMEOUT),
    EXTERNAL_API_ERROR("N002", "외부 API 호출 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    
    // ===== 비즈니스 로직 관련 에러 =====
    BUSINESS_RULE_VIOLATION("B001", "비즈니스 규칙을 위반했습니다.", HttpStatus.BAD_REQUEST),
    RESOURCE_CONFLICT("B002", "리소스 충돌이 발생했습니다.", HttpStatus.CONFLICT),
    OPERATION_NOT_ALLOWED("B003", "허용되지 않는 작업입니다.", HttpStatus.METHOD_NOT_ALLOWED);
    
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    
    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    public int getStatus() {
        return httpStatus.value();
    }
    
    /**
     * 에러 코드로 ErrorCode 찾기
     * @param code 에러 코드
     * @return 해당하는 ErrorCode, 없으면 INTERNAL_SERVER_ERROR 반환
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
    
    /**
     * HTTP 상태 코드로 기본 ErrorCode 찾기
     * @param httpStatus HTTP 상태 코드
     * @return 해당하는 기본 ErrorCode
     */
    public static ErrorCode fromHttpStatus(HttpStatus httpStatus) {
        return switch (httpStatus) {
            case BAD_REQUEST -> INVALID_REQUEST;
            case UNAUTHORIZED -> UNAUTHORIZED;
            case FORBIDDEN -> FORBIDDEN;
            case NOT_FOUND -> NOT_FOUND;
            case CONFLICT -> RESOURCE_CONFLICT;
            case INTERNAL_SERVER_ERROR -> INTERNAL_SERVER_ERROR;
            default -> INTERNAL_SERVER_ERROR;
        };
    }
}