package com.chareslm.shopping.common.exception;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.common.api.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception,
                                                                       HttpServletRequest request) {
        return ResponseEntity.status(resolveStatus(exception.getCode()))
                .body(ApiResponse.failure(exception.getCode(), exception.getMessage(), request.getHeader("X-Trace-Id")));
    }

    private static HttpStatus resolveStatus(int code) {
        return switch (code) {
            case 40101, 40102 -> HttpStatus.UNAUTHORIZED;
            case 40301, 40302, 40303 -> HttpStatus.FORBIDDEN;
            case 40401, 40406 -> HttpStatus.NOT_FOUND;
            case 40901, 40902, 40903, 40904 -> HttpStatus.CONFLICT;
            case 42301 -> HttpStatus.LOCKED;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception,
                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ErrorCode.FORBIDDEN, request.getHeader("X-Trace-Id")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException exception,
                                                                HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ErrorCode.NOT_FOUND, request.getHeader("X-Trace-Id")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception,
                                                                         HttpServletRequest request) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> describeField(error.getField(), error.getDefaultMessage()))
                .findFirst()
                .orElse(ErrorCode.VALIDATION_ERROR.message());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR.code(), detail, request.getHeader("X-Trace-Id")));
    }

    @ExceptionHandler({ConstraintViolationException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiResponse<Void>> handleRequestConstraint(Exception exception,
                                                                      HttpServletRequest request) {
        String detail = ErrorCode.VALIDATION_ERROR.message();
        if (exception instanceof ConstraintViolationException violationException) {
            detail = violationException.getConstraintViolations().stream()
                    .map(violation -> describeField(String.valueOf(violation.getPropertyPath()), violation.getMessage()))
                    .findFirst()
                    .orElse(detail);
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR.code(), detail, request.getHeader("X-Trace-Id")));
    }

    private static String describeField(String field, String defaultMessage) {
        String name = field == null ? "" : field;
        String simple = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;
        return switch (simple) {
            case "categoryId" -> "请选择有效的商品类目";
            case "name" -> "请填写商品名称";
            case "displayName" -> "请填写客服显示名";
            case "username" -> "用户名须以字母开头，3–64 位字母、数字或下划线";
            case "email" -> "请填写有效邮箱";
            case "skus" -> "请至少添加一个 SKU";
            case "price" -> "SKU 价格须大于 0";
            case "stock" -> "SKU 库存须为不小于 0 的整数";
            case "mainImage" -> "主图地址无效或过长";
            case "skuCode" -> "SKU 编码过长";
            default -> defaultMessage == null || defaultMessage.isBlank()
                    ? ErrorCode.VALIDATION_ERROR.message()
                    : defaultMessage;
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception,
                                                                         HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, request.getHeader("X-Trace-Id")));
    }
}
