package com.chareslm.shopping.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveUserAddressRequest(
        @NotBlank @Size(max = 64) String recipientName,
        @NotBlank @Pattern(regexp = "^[0-9+() -]{6,32}$") String recipientPhone,
        @Size(max = 32) String provinceCode,
        @NotBlank @Size(max = 64) String provinceName,
        @Size(max = 32) String cityCode,
        @NotBlank @Size(max = 64) String cityName,
        @Size(max = 32) String districtCode,
        @NotBlank @Size(max = 64) String districtName,
        @NotBlank @Size(max = 255) String detailAddress,
        @Size(max = 16) String postalCode,
        Boolean isDefault
) {
}
