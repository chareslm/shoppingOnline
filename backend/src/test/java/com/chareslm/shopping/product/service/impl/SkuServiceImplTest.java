package com.chareslm.shopping.product.service.impl;

import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import com.chareslm.shopping.product.entity.Sku;
import com.chareslm.shopping.product.entity.Spu;
import com.chareslm.shopping.product.mapper.SkuMapper;
import com.chareslm.shopping.product.mapper.SpuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkuServiceImplTest {
    private SkuMapper skuMapper;
    private SpuMapper spuMapper;
    private MerchantShopQueryService shops;
    private SkuServiceImpl service;

    @BeforeEach
    void setUp() {
        skuMapper = mock(SkuMapper.class);
        spuMapper = mock(SpuMapper.class);
        shops = mock(MerchantShopQueryService.class);
        service = new SkuServiceImpl(skuMapper, spuMapper, shops);
    }

    @Test
    void getReturnsOwnedSku() {
        when(skuMapper.selectById(5L)).thenReturn(sku(5L, 11L));
        when(spuMapper.selectById(11L)).thenReturn(spu(11L, 8L));
        Shop shop = new Shop();
        shop.setId(8L);
        when(shops.requireOpenShop(3L)).thenReturn(shop);

        assertEquals(5L, service.get(3L, 5L).id());
    }

    @Test
    void getRejectsOtherShopSku() {
        when(skuMapper.selectById(5L)).thenReturn(sku(5L, 11L));
        when(spuMapper.selectById(11L)).thenReturn(spu(11L, 99L));
        Shop shop = new Shop();
        shop.setId(8L);
        when(shops.requireOpenShop(3L)).thenReturn(shop);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.get(3L, 5L));
        assertEquals(ErrorCode.FORBIDDEN.code(), exception.getCode());
    }

    private static Sku sku(Long id, Long spuId) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setSpuId(spuId);
        sku.setPrice(java.math.BigDecimal.TEN);
        sku.setAvailableStock(1);
        sku.setReservedStock(0);
        sku.setSoldStock(0);
        sku.setStatus(1);
        return sku;
    }

    private static Spu spu(Long id, Long shopId) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setShopId(shopId);
        return spu;
    }
}
