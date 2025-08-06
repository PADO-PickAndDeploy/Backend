package com.pado.backend.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 프로젝트 전체에서 사용되는 커스텀 예외 클래스
 * ErrorCode Enum을 사용하여 표준화된 에러 처리를 제공합니다.
 */
public class CustomException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final String customMessage;
    
    /**
     * ErrorCode만으로 예외 생성 : default
     * @param errorCode 에러 코드
     */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }
    
    /**
     * ErrorCode와 커스텀 메시지로 예외 생성
     * @param errorCode 에러 코드
     * @param customMessage 커스텀 메시지 (ErrorCode의 기본 메시지를 오버라이드)
     */
    public CustomException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }
    
    /**
     * ErrorCode와 원인 예외로 예외 생성
     * @param errorCode 에러 코드
     * @param cause 원인 예외
     */
    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.customMessage = null;
    }
    
    /**
     * ErrorCode, 커스텀 메시지, 원인 예외로 예외 생성
     * @param errorCode 에러 코드
     * @param customMessage 커스텀 메시지
     * @param cause 원인 예외
     */
    public CustomException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }
    
    // ===== 기존 호환성을 위한 생성자 (deprecated), 마이그레이션 후 삭제 =====
    
    /**
     * @deprecated ErrorCode를 사용하는 생성자를 사용해주세요
     */
    @Deprecated
    public CustomException(String message, HttpStatus status) {
        super(message);
        this.errorCode = ErrorCode.fromHttpStatus(status);
        this.customMessage = message;
    }
    
    /**
     * @deprecated ErrorCode를 사용하는 생성자를 사용해주세요
     */
    @Deprecated
    public CustomException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.fromHttpStatus(status);
        this.customMessage = message;
    }
    
    // ===== Getter 메소드 =====
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getCode() {
        return errorCode.getCode();
    }
    
    public HttpStatus getStatus() {
        return errorCode.getHttpStatus();
    }
    
    public int getStatusValue() {
        return errorCode.getStatus();
    }
    
    /**
     * 실제 사용될 메시지 반환 (커스텀 메시지가 있으면 커스텀 메시지, 없으면 ErrorCode의 기본 메시지)
     */
    @Override
    public String getMessage() {
        return customMessage != null ? customMessage : errorCode.getMessage();
    }
    
    /**
     * ErrorCode의 기본 메시지 반환
     */
    public String getDefaultMessage() {
        return errorCode.getMessage();
    }
    
    /**
     * 커스텀 메시지 반환 (없으면 null)
     */
    public String getCustomMessage() {
        return customMessage;
    }
}