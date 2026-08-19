package com.chareslm.shopping.product.service.impl;

import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import com.chareslm.shopping.product.dto.response.ProductMediaResponse;
import com.chareslm.shopping.product.entity.ProductMedia;
import com.chareslm.shopping.product.mapper.ProductMediaMapper;
import com.chareslm.shopping.product.service.ProductImageStorage;
import com.chareslm.shopping.product.service.ProductMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ProductMediaServiceImpl implements ProductMediaService {
    private final ProductMediaMapper productMediaMapper;
    private final ProductImageStorage productImageStorage;
    private final MerchantShopQueryService merchantShopQueryService;

    @Override
    @Transactional
    public ProductMediaResponse upload(Long operatorId, MultipartFile file) {
        Shop shop = merchantShopQueryService.requireOpenShop(operatorId);
        ProductImageStorage.StoredFile stored = productImageStorage.store(file);
        try {
            ProductMedia media = new ProductMedia();
            media.setShopId(shop.getId());
            media.setStorageKey(stored.storageKey());
            media.setContentType(stored.contentType());
            media.setOriginalName(stored.originalName());
            media.setFileSize(stored.size());
            productMediaMapper.insert(media);
            return toResponse(media);
        } catch (RuntimeException exception) {
            productImageStorage.deleteQuietly(stored.storageKey());
            throw exception;
        }
    }

    @Override
    public ResponseEntity<Resource> download(Long mediaId) {
        ProductMedia media = productMediaMapper.selectById(mediaId);
        if (media == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Resource resource = productImageStorage.load(media.getStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(resource);
    }

    private static ProductMediaResponse toResponse(ProductMedia media) {
        return new ProductMediaResponse(media.getId(), "/api/product-media/" + media.getId(), media.getContentType());
    }
}
