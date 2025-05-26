// package com.pado.backend.global.config;

// import org.springframework.context.ApplicationContext;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Primary;
// import org.springframework.http.HttpStatus;


// import org.springframework.web.cors.CorsConfigurationSource;

// import lombok.RequiredArgsConstructor;

// @RequiredArgsConstructor
// @Configuration
// // @EnableWebSecurity
// // @EnableMethodSecurity(prePostEnabled = true) // TODO : @PreAuthorized 등 사용가능하다. , (prePostEnabled = true) 는 뭐임?
// public class SecurityConfig {
    
//     // private final CustomOAuth2UserService customOAuth2UserService;
//     // private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
//     // private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
//     // private final JwtAuthenticationFilter jwtAuthenticationFilter;
//     private final ApplicationContext context;
    
//     @Bean
//     @Primary 
//     PasswordEncoder passwordEncoder() {
//         return new BCryptPasswordEncoder();
//     }

//     @Bean
//     SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigSource) throws Exception {
//         http
//             .cors(cors -> cors.configurationSource(corsConfigSource))
//             .csrf(csrf -> csrf.disable()) // [x]: csrf.disable() -> REST API에서는 CSRF 비활성화가 일반적, 대신 JWT, OAuth2 등 토큰 기반 인증 방식 사용
//             // 현재 인증 방식 : JWT, 세션 저장이 필요 없는데 Spring Security는 기본적으로 세션에 인증 정보를 자동 저장하려고 시도함
//             .sessionManagement(session -> session
//             .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  
//             )
//             // [x] : Role 권한마다 접속 가능한 경로 지정
//             .authorizeHttpRequests(auth -> auth
//                 // 공개 API (비로그인 접근 허용)
//                 .requestMatchers(
//                     "/", 
//                     "**"
//                 ).permitAll()
//                 .anyRequest().authenticated()
//             )
//             .exceptionHandling(exception -> exception
//                 .authenticationEntryPoint((request, response, authException) -> {
//                     // 인증 실패 (401)
//                     response.setCharacterEncoding("UTF-8");
//                     response.setStatus(HttpStatus.UNAUTHORIZED.value());
//                     response.setContentType("application/json");
//                     response.getWriter().write("{\"error\": \"인증이 필요합니다\"}");
//                 })
//                 .accessDeniedHandler((request, response, accessDeniedException) -> {
//                     // 권한 부족 (403)
//                     response.setCharacterEncoding("UTF-8");
//                     response.setStatus(HttpStatus.FORBIDDEN.value());
//                     response.setContentType("application/json");
//                     response.getWriter().write("{\"error\": \"접근 권한이 없습니다\"}");
//                 })
//         )
//         // .addFilterBefore(jwtAuthenticationFilter, AnonymousAuthenticationFilter.class);
        
//         // // OAuth2 설정은 ClientRegistrationRepository 빈이 있을 때만 적용
//         // if (context.getBeanProvider(ClientRegistrationRepository.class).getIfAvailable() != null) {
//         //     http.oauth2Login(oauth2 -> oauth2
//         //         .userInfoEndpoint(userInfo -> 
//         //             userInfo.userService(customOAuth2UserService)
//         //         )
//         //         .successHandler(oAuth2LoginSuccessHandler)
//         //         .failureHandler(oAuth2LoginFailureHandler)
//         //     );
//         // } 
//         return http.build();
//     }
// }