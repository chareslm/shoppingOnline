package com.chareslm.shopping.merchant.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.merchant.dto.response.ShopSummaryResponse;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import com.chareslm.shopping.security.context.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/shop")
public class MerchantShopController {
    private final MerchantShopQueryService merchantShopQueryService;

    public MerchantShopController(MerchantShopQueryService merchantShopQueryService) {
        this.merchantShopQueryService = merchantShopQueryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('product:create','product:update','product:stock:adjust','merchant:staff:manage')")
    public ApiResponse<ShopSummaryResponse> current() {
        Shop shop = merchantShopQueryService.requireOpenShop(CurrentUser.require().userId());
        return ApiResponse.success(new ShopSummaryResponse(shop.getId(), shop.getName(), shop.getStatus()));
    }
}
