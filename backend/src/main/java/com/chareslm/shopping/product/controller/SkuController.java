package com.chareslm.shopping.product.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.product.dto.request.SkuStockAdjustRequest;
import com.chareslm.shopping.product.dto.request.SkuUpdateRequest;
import com.chareslm.shopping.product.dto.response.SkuResponse;
import com.chareslm.shopping.product.service.SkuService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/sku")
public class SkuController {

    private final SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    @GetMapping("/{skuId}")
    public ApiResponse<SkuResponse> get(@PathVariable Long skuId) {
        return ApiResponse.success(skuService.get(skuId));
    }

    @PutMapping("/{skuId}")
    @PreAuthorize("hasAuthority('product:update')")
    public ApiResponse<SkuResponse> update(@PathVariable Long skuId,
                                           @Valid @RequestBody SkuUpdateRequest request) {
        return ApiResponse.success(skuService.update(CurrentUser.require().userId(), skuId, request));
    }

    @PutMapping("/{skuId}/stock")
    @PreAuthorize("hasAuthority('product:stock:adjust')")
    public ApiResponse<SkuResponse> adjustStock(@PathVariable Long skuId,
                                                @Valid @RequestBody SkuStockAdjustRequest request) {
        return ApiResponse.success(skuService.adjustStock(CurrentUser.require().userId(), skuId, request));
    }
}
