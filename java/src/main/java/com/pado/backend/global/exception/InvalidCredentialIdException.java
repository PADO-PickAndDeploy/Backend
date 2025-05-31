package com.pado.backend.global.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialIdException extends CustomException {
    public InvalidCredentialIdException() {
        super("유효하지 않은 Credential ID입니다.", HttpStatus.BAD_REQUEST);
    }
}