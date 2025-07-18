package com.pado.backend.global.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 프로젝트 전체에서 발생하는 예외들을 전역적으로 처리 해주는 클래스
/*
 * TODO : 현재 너무 많은 예외 클래스들을 생성하고 있음, 나중에 관리가 어려울 것 같고
 * 클래스명만 다르지 같은 예외 처리를 하는 클래스들을 생성할 수 있을 것 같단 생각이 듦.
 * 1. Enum을 활용한 에러 코드 관리
 * 2. 표준화된 에러 응답 DTO(ErrorResponseDto)
 */ 
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(CustomException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        error.put("status", e.getStatus().value());
        error.put("error", e.getStatus().getReasonPhrase());
        error.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(e.getStatus()).body(error);
    
    }

    /**
     * 유효성 검증 실패 처리 (@Valid 어노테이션에서 발생)
     * 여기서 @NotBlank, @Size, @Pattern, @Email 등의 메시지가 처리됨
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        // 각 필드별 에러 메시지 추출
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("message", "입력 데이터 검증에 실패했습니다.");
        response.put("errors", errors);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(response);
    }
    
}

