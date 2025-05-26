package com.pado.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {
    @Bean
    @Primary
    /*
     * @Primary로 우선순위 지정, 왜 하는지? 자동 주입되는 mvcHandlerMappingIntrospector이
     * CorsConfigurationSource과 빈 충돌을 일으키므로
     */
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173")); // 허용할 Origin
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // 허용할 HTTP Method
        config.setAllowedHeaders(Arrays.asList("*")); // 허용할 Header
        config.setAllowCredentials(true); // 인증정보 허용 여부
        config.setExposedHeaders(Arrays.asList("Authorization")); // 노출할 헤더

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 모든 경로에 적용
        return source;
    }
}
