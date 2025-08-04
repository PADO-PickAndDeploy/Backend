package com.pado.backend.global.type;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ComponentStatus {
    // TODO : RUNNING -> DEPLOY
    RUNNING(0),
    START(1),
    ERROR(2),
    DRAFT(3);

    private final int code;

    ComponentStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @JsonValue
    public int toJson() {
        return code;
    }
}