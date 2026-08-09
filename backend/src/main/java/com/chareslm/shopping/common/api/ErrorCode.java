package com.chareslm.shopping.common.api;

public enum ErrorCode {
    SUCCESS(0, "success"),
    VALIDATION_ERROR(40001, "request validation failed"),
    ACCOUNT_ALREADY_EXISTS(40901, "account already exists"),
    INVALID_CREDENTIALS(40102, "invalid credentials"),
    ACCOUNT_DISABLED(40302, "account is disabled"),
    ACCOUNT_LOCKED(42301, "account is temporarily locked"),
    UNAUTHORIZED(40101, "authentication required"),
    FORBIDDEN(40301, "permission denied"),
    NOT_FOUND(40401, "resource not found"),
    INTERNAL_ERROR(50000, "internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
