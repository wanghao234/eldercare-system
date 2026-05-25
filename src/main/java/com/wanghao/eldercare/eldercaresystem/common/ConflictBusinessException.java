package com.wanghao.eldercare.eldercaresystem.common;

import org.springframework.http.HttpStatus;

public class ConflictBusinessException extends BusinessException {

    private final Object data;

    public ConflictBusinessException(String code, String message, Object data) {
        super(code, message, HttpStatus.BAD_REQUEST);
        this.data = data;
    }

    public Object getData() {
        return data;
    }
}
