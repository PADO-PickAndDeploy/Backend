package com.pado.backend.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 유효하지 않은 인증 정보일 때 발생하는 예외
 */
public class InvalidAuthenticationException extends CustomException {
    public InvalidAuthenticationException() {
        super("유효하지 않은 인증 정보입니다.", HttpStatus.UNAUTHORIZED);
    }
}

