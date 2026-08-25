package com.chareslm.shopping.product.service.impl;

import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.mapper.ShopMapper;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import com.chareslm.shopping.product.dto.request.SpuAuditRequest;
import com.chareslm.shopping.product.dto.request.SpuCreateRequest;
import com.chareslm.shopping.product.dto.request.SpuStatusRequest;
import com.chareslm.shopping.product.dto.request.SkuCreateRequest;
import com.chareslm.shopping.product.entity.Spu;
import com.chareslm.shopping.product.enums.SpuStatus;
import com.chareslm.shopping.product.event.ProductChangedEvent;
import com.chareslm.shopping.product.mapper.ProductStatusLogMapper;
import com.chareslm.shopping.product.mapper.SkuMapper;
import com.chareslm.shopping.product.mapper.SpuMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpuServiceImplTest {
    private SpuMapper spuMapper;
    private SkuMapper skuMapper;
    private MerchantShopQueryService shops;
    private ShopMapper shopMapper;
    private SpuServiceImpl service;

    @BeforeEach
    void setUp() {
        spuMapper = mock(SpuMapper.class);
        skuMapper = mock(SkuMapper.class);
        shops = mock(MerchantShopQueryService.class);
        shopMapper = mock(ShopMapper.class);
        Shop listed = new Shop();
        listed.setId(9L);
        listed.setName("旗舰店");
        when(shopMapper.selectById(9L)).thenReturn(listed);
        when(skuMapper.selectList(any())).thenReturn(List.of());
        service = new SpuServiceImpl(spuMapper, skuMapper, mock(ProductStatusLogMapper.class),
                mock(com.chareslm.shopping.product.mapper.CategoryMapper.class),
                shopMapper,
                shops, mock(ObjectMapper.class), mock(ApplicationEventPublisher.class));
    }

    @Test
    void createUsesOpenShopFromAccount() {
        Shop shop = new Shop();
        shop.setId(9L);
        shop.setStatus("OPEN");
        when(shops.requireOpenShop(3L)).thenReturn(shop);
        when(spuMapper.insert(any(Spu.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Spu.class).setId(11L);
            return 1;
        });

        var created = service.create(3L, new SpuCreateRequest("1", "Brand", "Phone", null, null, null, null,
                List.of(new SkuCreateRequest("A", null, null, new BigDecimal("10"), new BigDecimal("5")))));

        assertEquals("DRAFT", created.status());
        verify(spuMapper).insert(any(Spu.class));
    }

    @Test
    void auditApprovePutsPendingProductOnSale() {
        when(spuMapper.selectById(11L)).thenReturn(spu("PENDING_AUDIT"));

        var result = service.audit(8L, 11L, new SpuAuditRequest("APPROVE", "ok"));

        assertEquals("ON_SALE", result.status());
        assertEquals("旗舰店", result.shopName());
    }

    @Test
    void auditRevokeRemovesApprovedListing() {
        when(spuMapper.selectById(11L)).thenReturn(spu("ON_SALE"));

        var result = service.audit(8L, 11L, new SpuAuditRequest("REVOKE", "recalled"));

        assertEquals("AUDIT_REJECTED", result.status());
    }

    @Test
    void auditApproveRestoresRevokedProduct() {
        when(spuMapper.selectById(11L)).thenReturn(spu("AUDIT_REJECTED"));

        var result = service.audit(8L, 11L, new SpuAuditRequest("APPROVE", "again"));

        assertEquals("ON_SALE", result.status());
    }

    @Test
    void merchantCannotPublishBeforeAudit() {
        Shop shop = new Shop();
        shop.setId(9L);
        when(shops.requireOpenShop(3L)).thenReturn(shop);
        Spu pending = spu("PENDING_AUDIT");
        pending.setShopId(9L);
        when(spuMapper.selectById(11L)).thenReturn(pending);

        assertThrows(BusinessException.class,
                () -> service.changeStatus(3L, 11L, new SpuStatusRequest("PUBLISH", null)));
    }

    private Spu spu(String status) {
        Spu spu = new Spu();
        spu.setId(11L);
        spu.setShopId(9L);
        spu.setName("Phone");
        spu.setStatus(status);
        return spu;
    }
}
