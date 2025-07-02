package com.pado.backend.global.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(CustomException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        error.put("status", e.getStatus().value());
        error.put("error", e.getStatus().getReasonPhrase());
        error.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(e.getStatus()).body(error);
    
    }

    
}

