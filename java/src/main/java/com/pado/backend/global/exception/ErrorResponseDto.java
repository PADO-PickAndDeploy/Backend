package com.pado.backend.global.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** TODO: DTO를 만들어주는게 더 좋다고 하길래 일단 만들어 두기만 한거고 추후 필요한 경우에 사용할 것, 
 * 표준화된 에러 응답 DTO
 * 모든 예외 처리에서 일관된 형태의 에러 응답을 제공
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto {
    
    /**
     * 에러 코드 (예: "CP001", "J001")
     */
    private final String code;
    
    /**
     * 사용자에게 보여질 에러 메시지
     */
    private final String message;
    
    /**
     * HTTP 상태 코드 (예: 400, 401, 500)
     */
    private final int status;
    
    /**
     * HTTP 상태 설명 (예: "Bad Request", "Unauthorized")
     */
    private final String error;
    
    /**
     * 요청 경로
     */
    private final String path;
    
    /**
     * 에러 발생 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;
    
    /**
     * 개발자를 위한 상세 정보 (개발 환경에서만 포함)
     */
    private final String details;
    
    /**
     * 추적 ID (로그 추적용)
     */
    private final String traceId;
    
    /**
     * CustomException으로부터 ErrorResponseDto 생성
     * @param exception CustomException
     * @param path 요청 경로
     * @return ErrorResponseDto
     */
    public static ErrorResponseDto from(CustomException exception, String path) {
        return ErrorResponseDto.builder()
                .code(exception.getCode())
                .message(exception.getMessage())
                .status(exception.getStatusValue())
                .error(exception.getStatus().getReasonPhrase())
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * CustomException으로부터 ErrorResponseDto 생성 (상세 정보 포함)
     * @param exception CustomException
     * @param path 요청 경로
     * @param details 상세 정보
     * @return ErrorResponseDto
     */
    public static ErrorResponseDto from(CustomException exception, String path, String details) {
        return ErrorResponseDto.builder()
                .code(exception.getCode())
                .message(exception.getMessage())
                .status(exception.getStatusValue())
                .error(exception.getStatus().getReasonPhrase())
                .path(path)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();
    }
    
    /**
     * ErrorCode로부터 ErrorResponseDto 생성
     * @param errorCode ErrorCode
     * @param path 요청 경로
     * @return ErrorResponseDto
     */
    public static ErrorResponseDto from(ErrorCode errorCode, String path) {
        return ErrorResponseDto.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * ErrorCode로부터 ErrorResponseDto 생성 (커스텀 메시지 포함)
     * @param errorCode ErrorCode
     * @param customMessage 커스텀 메시지
     * @param path 요청 경로
     * @return ErrorResponseDto
     */
    public static ErrorResponseDto from(ErrorCode errorCode, String customMessage, String path) {
        return ErrorResponseDto.builder()
                .code(errorCode.getCode())
                .message(customMessage)
                .status(errorCode.getStatus())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 일반 Exception으로부터 ErrorResponseDto 생성
     * @param exception Exception
     * @param errorCode 적용할 ErrorCode
     * @param path 요청 경로
     * @return ErrorResponseDto
     */
    public static ErrorResponseDto from(Exception exception, ErrorCode errorCode, String path) {
        return ErrorResponseDto.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .path(path)
                .timestamp(LocalDateTime.now())
                .details(exception.getMessage())
                .build();
    }
}