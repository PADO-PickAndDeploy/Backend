package com.pado.backend.global.exception;

import org.springframework.http.HttpStatus;
// TODO : 수정이 필요할 듯? RUNNING 상태의 컴포넌트도 삭제할 수 있을지도 모름 
public class ComponentDeletionNotAllowedException extends CustomException {
    public ComponentDeletionNotAllowedException(){
        super("RUNNING 상태의 컴포넌트는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}
