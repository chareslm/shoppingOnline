package com.chareslm.shopping.common.api;

/** Standard response body for every REST API. */
public record ApiResponse<T>(int code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data, null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String traceId) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null, traceId);
    }

    public static <T> ApiResponse<T> failure(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
