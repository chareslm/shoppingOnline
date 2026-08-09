package com.chareslm.shopping.common.exception;

import com.chareslm.shopping.common.api.ErrorCode;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.code(), errorCode.message());
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
