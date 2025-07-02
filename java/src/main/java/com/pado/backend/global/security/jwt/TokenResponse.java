package com.pado.backend.global.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Data;

/*
 * TODO : 현재 방식은 프론트에서 Token을 삭제해서 로그아웃 하는 형식임. 
 * 백에서는 로그만 기록함.
 * 백에서 로그아웃 기능을 구현하려면 엔티티를 사용, RefreshToken을 DB에 저장이 필요
 */
@Data
@AllArgsConstructor
public class TokenResponse {
    private final String accessToken;
    private final String refreshToken;
}