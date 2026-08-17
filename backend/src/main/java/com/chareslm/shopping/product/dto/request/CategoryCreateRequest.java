package com.chareslm.shopping.product.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @Min(0) Long parentId,
        @NotBlank @Size(max = 64) String name,
        @Min(1) @Max(3) Integer level,
        @Min(0) Integer sortOrder,
        @Size(max = 512) String icon,
        @Min(0) @Max(1) Integer status
) {
}
