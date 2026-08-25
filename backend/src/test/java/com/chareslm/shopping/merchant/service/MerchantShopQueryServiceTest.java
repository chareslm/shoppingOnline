package com.chareslm.shopping.merchant.service;

import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.entity.ShopStaff;
import com.chareslm.shopping.merchant.mapper.ShopMapper;
import com.chareslm.shopping.merchant.mapper.ShopStaffMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MerchantShopQueryServiceTest {
    private ShopMapper shopMapper;
    private ShopStaffMapper shopStaffMapper;
    private MerchantShopQueryService service;

    @BeforeEach
    void setUp() {
        shopMapper = mock(ShopMapper.class);
        shopStaffMapper = mock(ShopStaffMapper.class);
        service = new MerchantShopQueryService(shopMapper, shopStaffMapper);
    }

    @Test
    void ownerOpenShopIsReturned() {
        Shop shop = openShop(8L);
        when(shopMapper.selectByOwnerUserId(3L)).thenReturn(shop);

        assertEquals(8L, service.requireOpenShop(3L).getId());
    }

    @Test
    void activeStaffResolvesShopWhenNotOwner() {
        ShopStaff staff = new ShopStaff();
        staff.setShopId(8L);
        staff.setStatus("ACTIVE");
        when(shopMapper.selectByOwnerUserId(21L)).thenReturn(null);
        when(shopStaffMapper.selectActiveByUserId(21L)).thenReturn(staff);
        when(shopMapper.selectById(8L)).thenReturn(openShop(8L));

        assertEquals("旗舰店", service.requireOpenShop(21L).getName());
    }

    @Test
    void missingShopUsesChineseMessage() {
        when(shopMapper.selectByOwnerUserId(3L)).thenReturn(null);
        when(shopStaffMapper.selectActiveByUserId(3L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.requireOpenShop(3L));
        assertEquals(ErrorCode.FORBIDDEN.code(), exception.getCode());
        assertEquals("尚未开通店铺。请先完成商家入驻，并等待平台审核通过后再上传商品。", exception.getMessage());
    }

    @Test
    void openShopCanBeValidatedAsPublicChatTarget() {
        when(shopMapper.selectById(8L)).thenReturn(openShop(8L));

        assertEquals(8L, service.requireOpenShopById(8L).getId());
    }

    @Test
    void closedOrUnknownShopIsHiddenFromPublicChatTarget() {
        Shop suspended = openShop(8L);
        suspended.setStatus("SUSPENDED");
        when(shopMapper.selectById(8L)).thenReturn(suspended);

        BusinessException suspendedError = assertThrows(BusinessException.class,
                () -> service.requireOpenShopById(8L));
        BusinessException missingError = assertThrows(BusinessException.class,
                () -> service.requireOpenShopById(9L));
        assertEquals(ErrorCode.NOT_FOUND.code(), suspendedError.getCode());
        assertEquals(ErrorCode.NOT_FOUND.code(), missingError.getCode());
    }

    private static Shop openShop(Long id) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName("旗舰店");
        shop.setStatus("OPEN");
        return shop;
    }
}
