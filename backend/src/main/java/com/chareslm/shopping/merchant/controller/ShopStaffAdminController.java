package com.chareslm.shopping.merchant.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.merchant.dto.request.ShopStaffAuditRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/admin/merchant/staff")
@PreAuthorize("hasAuthority('merchant:staff:audit')")
public class ShopStaffAdminController {
    private final ShopStaffService shopStaffService;

    public ShopStaffAdminController(ShopStaffService shopStaffService) {
        this.shopStaffService = shopStaffService;
    }

    @GetMapping
    public ApiResponse<List<ShopStaffResponse>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(shopStaffService.listForAdmin(status));
    }

    @PostMapping("/{staffId}/audit")
    public ApiResponse<ShopStaffResponse> audit(@PathVariable Long staffId,
                                                @Valid @RequestBody ShopStaffAuditRequest request) {
        return ApiResponse.success(shopStaffService.audit(CurrentUser.require().userId(), staffId, request));
    }

    @PostMapping("/{staffId}/revoke")
    public ApiResponse<ShopStaffResponse> revoke(@PathVariable Long staffId,
                                                 @RequestBody(required = false) Map<String, String> body) {
        String remark = body == null ? null : body.get("remark");
        return ApiResponse.success(shopStaffService.revoke(CurrentUser.require().userId(), staffId, remark));
    }

    @PostMapping("/{staffId}/restore")
    public ApiResponse<ShopStaffResponse> restore(@PathVariable Long staffId,
                                                  @RequestBody(required = false) Map<String, String> body) {
        String remark = body == null ? null : body.get("remark");
        return ApiResponse.success(shopStaffService.restore(CurrentUser.require().userId(), staffId, remark));
    }

    @PostMapping("/{staffId}/credential-email/retry")
    public ApiResponse<ShopStaffResponse> retryEmail(@PathVariable Long staffId) {
        return ApiResponse.success(shopStaffService.retryCredentialEmailForAdmin(CurrentUser.require().userId(), staffId));
    }
}
