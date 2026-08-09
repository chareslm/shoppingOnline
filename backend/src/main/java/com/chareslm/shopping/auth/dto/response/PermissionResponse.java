package com.chareslm.shopping.auth.dto.response;

public record PermissionResponse(Long id, String code, String name, String resource, String action,
                                 String description) {
}
