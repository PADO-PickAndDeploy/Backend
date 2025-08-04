package com.pado.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;


/**
 * CORS 설정 - 실제 서비스용
 * 
 * SOP (Same-Origin Policy): 브라우저 보안 정책으로 다른 출처 접근 차단
 * CORS (Cross-Origin Resource Sharing): 서버가 허용한 다른 출처에서의 접근 허가
 * 
 * @Primary: mvcHandlerMappingIntrospector와의 빈 충돌 방지
 */
@Configuration
public class CorsConfig {
    
    // 환경별 설정 파일로 우리 서버에 접근 가능한 Origin 정리
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;
    
    @Bean
    @Primary
    /*
     * @Primary : 스프링이 자동으로 생성하는 빈 말고 우선순위를 먼저하여 빈을 생성하도록 함.
     * SpringBoot에서 mvcHandlerMappingIntrospector가 자동으로 CorsConfigurationSource 빈을 만들어서 빈 충돌이
     * 발생하는 문제를 해결시켜준다.
     */
    public CorsConfigurationSource corsConfigurationSource(){
        /*
         * TODO : 추후 설정 사항 변경 가능
         * 어떤 Origin은 어떤 메서드만 허용 가능하다든지, 우리 서버의 어떤 API에만 접근 가능하다든지 등
         */
        
        // CORS 설정을 위한 객체 생성
        CorsConfiguration config = new CorsConfiguration();
        
        // 실제 CORS 규칙들을 설정함.
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(Arrays.asList("Authorization"));

        // URL 패턴 별 설정 관리자 생성
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 우리 서버의 모든 경로('/**'), 즉 API에 접근 가능하다는 의미
        source.registerCorsConfiguration("/**", config); 
        return source;
    }
}
