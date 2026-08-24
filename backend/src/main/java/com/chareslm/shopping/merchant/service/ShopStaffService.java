package com.chareslm.shopping.merchant.service;

import com.chareslm.shopping.merchant.dto.request.CreateShopStaffRequest;
import com.chareslm.shopping.merchant.dto.request.ShopStaffAuditRequest;
import com.chareslm.shopping.merchant.dto.response.ShopStaffResponse;

import java.util.List;

public interface ShopStaffService {
    List<ShopStaffResponse> list(Long ownerUserId);

    ShopStaffResponse create(Long ownerUserId, CreateShopStaffRequest request);

    ShopStaffResponse retryCredentialEmail(Long ownerUserId, Long staffId);

    List<ShopStaffResponse> listForAdmin(String status);

    ShopStaffResponse audit(Long operatorId, Long staffId, ShopStaffAuditRequest request);

    ShopStaffResponse revoke(Long operatorId, Long staffId, String remark);

    ShopStaffResponse restore(Long operatorId, Long staffId, String remark);

    ShopStaffResponse retryCredentialEmailForAdmin(Long operatorId, Long staffId);
}
