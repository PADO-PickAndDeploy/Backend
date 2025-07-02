package com.pado.backend.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 인증 실패 시 처리하는 Entry Point
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 401 Unauthorized 응답을 반환
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 인증 실패 시 호출되는 메소드
     * 
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param authException 인증 예외
     * @throws IOException 입출력 예외
     * @throws ServletException 서블릿 예외
     */
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // 로그 기록
        log.warn("인증되지 않은 요청 감지 - URI: {} {}, Message: {}", 
                method, requestURI, authException.getMessage());
        
        // 응답 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // 에러 응답 본문 생성
        Map<String, Object> errorResponse = createErrorResponse(authException, requestURI);
        
        // JSON 응답 작성
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    /**
     * 일관된 에러 응답 형식 생성
     * 
     * @param authException 인증 예외
     * @param requestURI 요청 URI
     * @return 에러 응답 맵
     */
    private Map<String, Object> createErrorResponse(AuthenticationException authException, String requestURI) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("status", 401);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", determineErrorMessage(authException));
        errorResponse.put("path", requestURI);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return errorResponse;
    }
    
    /**
     * 예외 타입에 따른 적절한 에러 메시지 결정
     * 
     * @param authException 인증 예외
     * @return 사용자 친화적인 에러 메시지
     */
    private String determineErrorMessage(AuthenticationException authException) {
        String exceptionMessage = authException.getMessage();
        
        // JWT 토큰 관련 에러 메시지 분류
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("expired")) {
                return "토큰이 만료되었습니다. 다시 로그인해주세요.";
            } else if (exceptionMessage.contains("malformed") || exceptionMessage.contains("invalid")) {
                return "유효하지 않은 토큰입니다.";
            } else if (exceptionMessage.contains("signature")) {
                return "토큰 서명이 유효하지 않습니다.";
            }
        }
        
        // 기본 메시지
        return "인증이 필요합니다. 로그인 후 다시 시도해주세요.";
    }
}