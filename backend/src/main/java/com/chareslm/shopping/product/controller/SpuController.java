package com.chareslm.shopping.product.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.product.dto.request.SkuCreateRequest;
import com.chareslm.shopping.product.dto.request.SpuAuditRequest;
import com.chareslm.shopping.product.dto.request.SpuCreateRequest;
import com.chareslm.shopping.product.dto.request.SpuStatusRequest;
import com.chareslm.shopping.product.dto.request.SpuUpdateRequest;
import com.chareslm.shopping.product.dto.response.SpuDetailResponse;
import com.chareslm.shopping.product.dto.response.SpuResponse;
import com.chareslm.shopping.product.service.SpuService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
public class SpuController {

    private final SpuService spuService;

    public SpuController(SpuService spuService) {
        this.spuService = spuService;
    }

    /** 商品详情（用户端，公开，仅上架商品可见）。 */
    @GetMapping("/spu/{spuId}")
    public ApiResponse<SpuDetailResponse> getPublicDetail(@PathVariable Long spuId) {
        SpuDetailResponse detail = spuService.getDetail(spuId);
        if (!"ON_SALE".equals(detail.status())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return ApiResponse.success(detail);
    }

    /** 商品分页列表（用户端，公开，仅上架商品）。 */
    @GetMapping("/spu/page")
    public ApiResponse<PageResponse<SpuResponse>> publicPage(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize) {
        return ApiResponse.success(spuService.page(categoryId, keyword, "ON_SALE", page, pageSize));
    }

    /** 商家创建商品（SPU + 首个 SKU 列表）。 */
    @PostMapping("/merchant/spu")
    @PreAuthorize("hasAuthority('product:create')")
    public ApiResponse<SpuDetailResponse> create(@Valid @RequestBody SpuCreateRequest request) {
        return ApiResponse.success(spuService.create(CurrentUser.require().userId(), request));
    }

    /** 商家编辑商品基础信息。 */
    @PutMapping("/merchant/spu/{spuId}")
    @PreAuthorize("hasAuthority('product:update')")
    public ApiResponse<SpuDetailResponse> update(@PathVariable Long spuId,
                                                 @Valid @RequestBody SpuUpdateRequest request) {
        return ApiResponse.success(spuService.update(CurrentUser.require().userId(), spuId, request));
    }

    /** 商家提交审核 / 上架 / 下架。 */
    @PutMapping("/merchant/spu/{spuId}/status")
    @PreAuthorize("hasAuthority('product:update')")
    public ApiResponse<SpuResponse> changeStatus(@PathVariable Long spuId,
                                                 @Valid @RequestBody SpuStatusRequest request) {
        return ApiResponse.success(spuService.changeStatus(CurrentUser.require().userId(), spuId, request));
    }

    /** 商家为已有商品追加 SKU。 */
    @PostMapping("/merchant/spu/{spuId}/sku")
    @PreAuthorize("hasAuthority('product:update')")
    public ApiResponse<SpuDetailResponse> addSku(@PathVariable Long spuId,
                                                 @Valid @RequestBody SkuCreateRequest request) {
        return ApiResponse.success(spuService.addSku(CurrentUser.require().userId(), spuId, request));
    }

    /** 管理员审核商品。 */
    @PutMapping("/admin/spu/{spuId}/audit")
    @PreAuthorize("hasAuthority('product:audit')")
    public ApiResponse<SpuResponse> audit(@PathVariable Long spuId,
                                          @Valid @RequestBody SpuAuditRequest request) {
        return ApiResponse.success(spuService.audit(CurrentUser.require().userId(), spuId, request));
    }

    /** 管理员按状态分页查询商品（管理端商品列表）。 */
    @GetMapping("/admin/spu/page")
    @PreAuthorize("hasAuthority('product:audit')")
    public ApiResponse<PageResponse<SpuResponse>> adminPage(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize) {
        return ApiResponse.success(spuService.page(categoryId, keyword, status, page, pageSize));
    }
}
