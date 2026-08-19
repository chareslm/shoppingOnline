package com.chareslm.shopping.merchant.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.merchant.dto.request.CreateShopStaffRequest;
import com.chareslm.shopping.merchant.dto.response.ShopStaffResponse;
import com.chareslm.shopping.merchant.service.ShopStaffService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/merchant/staff")
@PreAuthorize("hasAuthority('merchant:staff:manage')")
public class ShopStaffController {
    private final ShopStaffService shopStaffService;

    public ShopStaffController(ShopStaffService shopStaffService) {
        this.shopStaffService = shopStaffService;
    }

    @GetMapping
    public ApiResponse<List<ShopStaffResponse>> list() {
        return ApiResponse.success(shopStaffService.list(CurrentUser.require().userId()));
    }

    @PostMapping
    public ApiResponse<ShopStaffResponse> create(@Valid @RequestBody CreateShopStaffRequest request) {
        return ApiResponse.success(shopStaffService.create(CurrentUser.require().userId(), request));
    }

    @PostMapping("/{staffId}/credential-email/retry")
    public ApiResponse<ShopStaffResponse> retryEmail(@PathVariable Long staffId) {
        return ApiResponse.success(shopStaffService.retryCredentialEmail(CurrentUser.require().userId(), staffId));
    }
}
