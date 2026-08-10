package com.chareslm.shopping.common.api;

public enum ErrorCode {
    SUCCESS(0, "success"),
    VALIDATION_ERROR(40001, "request validation failed"),
    UNAUTHORIZED(40101, "authentication required"),
    FORBIDDEN(40301, "permission denied"),
    NOT_FOUND(40401, "resource not found"),
    INTERNAL_ERROR(50000, "internal server error"),
    CART_EMPTY(40010, "no checked items to checkout"),
    STOCK_NOT_ENOUGH(40011, "insufficient stock"),
    ORDER_STATUS_INVALID(40012, "order status not allowed"),
    ORDER_NOT_FOUND(40013, "order not found"),
    PAYMENT_ALREADY_PROCESSED(40014, "payment already processed"),
    PAYMENT_NOT_FOUND(40015, "payment order not found"),
    REFUND_STATUS_INVALID(40016, "refund status not allowed");

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
