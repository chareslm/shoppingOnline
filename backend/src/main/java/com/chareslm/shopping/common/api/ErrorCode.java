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
    PASSWORD_CHANGE_REQUIRED(40303, "password change required"),
    NOT_FOUND(40401, "resource not found"),
    INTERNAL_ERROR(50000, "internal server error"),
    CART_EMPTY(40010, "no checked items to checkout"),
    STOCK_NOT_ENOUGH(40011, "insufficient stock"),
    ORDER_STATUS_INVALID(40012, "order status not allowed"),
    ORDER_NOT_FOUND(40013, "order not found"),
    PAYMENT_ALREADY_PROCESSED(40014, "payment already processed"),
    PAYMENT_NOT_FOUND(40015, "payment order not found"),
    REFUND_STATUS_INVALID(40016, "refund status not allowed"),
    CATEGORY_NOT_FOUND(40402, "category not found"),
    CATEGORY_HAS_CHILDREN(40020, "category has children and cannot be deleted"),
    PRODUCT_NOT_FOUND(40403, "product not found"),
    PRODUCT_STATUS_INVALID(40021, "product status not allowed"),
    SKU_NOT_FOUND(40404, "sku not found"),
    REVIEW_NOT_FOUND(40405, "review not found"),
    REVIEW_ALREADY_EXISTS(40022, "review already exists"),
    REVIEW_NOT_ELIGIBLE(40023, "review not eligible"),
    SEARCH_SERVICE_UNAVAILABLE(50001, "search service unavailable"),
    MERCHANT_APPLICATION_CONFLICT(40902, "merchant application state conflict"),
    SKU_CODE_DUPLICATE(40903, "sku code already exists"),
    MERCHANT_FILE_INVALID(40030, "invalid qualification file"),
    MAIL_SEND_FAILED(40031, "mail send failed"),
    PRODUCT_IMAGE_INVALID(40032, "invalid product image");

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
