package com.pado.backend.global.security.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pado.backend.global.security.userdetails.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 토큰을 검증하고 SecurityContext에 인증 정보를 설정하는 필터
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. Request Header에서 JWT 토큰 추출
            String jwt = getJwtFromRequest(request);
            
            // 2. JWT 토큰 유효성 검증
            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
                
                // 3. JWT에서 사용자 이메일 추출
                String email = jwtUtil.extractEmail(jwt);
                
                // 4. 사용자 정보 조회
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                // 5. Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        userDetails.getAuthorities()
                    );
                
                // 6. 요청 정보 설정
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 7. SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("사용자 인증 성공: {}", email);
            }
            
        } catch (Exception e) {
            log.warn("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
            // 인증 실패 시 SecurityContext 초기화
            SecurityContextHolder.clearContext();
        }
        
        // 8. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
    
    /**
     * Request Header에서 JWT 토큰 추출
     * 
     * @param request HTTP 요청
     * @return JWT 토큰 문자열 (Bearer 제거된 상태)
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }
        
        return null;
    }
    
    /**
     * 특정 경로에 대해 필터 적용을 제외할지 결정
     * 
     * @param request HTTP 요청
     * @return true면 필터 적용 안 함, false면 필터 적용
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // 인증이 필요 없는 경로들은 필터 적용 안 함
        return path.equals("/signup") || 
               path.equals("/signin") || 
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/components") ||
               path.startsWith("/components/search");
    }
}