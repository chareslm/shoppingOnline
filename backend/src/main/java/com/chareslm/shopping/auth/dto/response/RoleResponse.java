package com.chareslm.shopping.auth.dto.response;

public record RoleResponse(Long id, String code, String name, String dataScope, boolean builtIn) {
}
