package com.chareslm.shopping.merchant.service;

import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.entity.ShopStaff;
import com.chareslm.shopping.merchant.mapper.ShopMapper;
import com.chareslm.shopping.merchant.mapper.ShopStaffMapper;
import org.springframework.stereotype.Service;

/**
 * 向其他模块提供当前账号的有效店铺，不信任客户端提交的 shopId。
 */
@Service
public class MerchantShopQueryService {
    private final ShopMapper shopMapper;
    private final ShopStaffMapper shopStaffMapper;

    public MerchantShopQueryService(ShopMapper shopMapper, ShopStaffMapper shopStaffMapper) {
        this.shopMapper = shopMapper;
        this.shopStaffMapper = shopStaffMapper;
    }

    public Shop requireOpenShop(Long userId) {
        Shop shop = shopMapper.selectByOwnerUserId(userId);
        if (shop == null) {
            ShopStaff staff = shopStaffMapper.selectActiveByUserId(userId);
            if (staff != null) {
                shop = shopMapper.selectById(staff.getShopId());
            }
        }
        if (shop == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(),
                    "尚未开通店铺。请先完成商家入驻，并等待平台审核通过后再上传商品。");
        }
        if (!"OPEN".equals(shop.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(),
                    "店铺当前不可经营，无法添加商品或上传图片。");
        }
        return shop;
    }
}
