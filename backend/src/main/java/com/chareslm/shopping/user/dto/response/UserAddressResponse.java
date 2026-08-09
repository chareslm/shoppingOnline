package com.chareslm.shopping.user.dto.response;

public record UserAddressResponse(
        Long id,
        String recipientName,
        String recipientPhone,
        String provinceCode,
        String provinceName,
        String cityCode,
        String cityName,
        String districtCode,
        String districtName,
        String detailAddress,
        String postalCode,
        boolean isDefault
) {
}
