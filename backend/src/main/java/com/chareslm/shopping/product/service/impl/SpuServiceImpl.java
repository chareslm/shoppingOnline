package com.chareslm.shopping.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.product.dto.request.SkuCreateRequest;
import com.chareslm.shopping.product.dto.request.SpuAuditRequest;
import com.chareslm.shopping.product.dto.request.SpuCreateRequest;
import com.chareslm.shopping.product.dto.request.SpuStatusRequest;
import com.chareslm.shopping.product.dto.request.SpuUpdateRequest;
import com.chareslm.shopping.product.dto.response.SkuResponse;
import com.chareslm.shopping.product.dto.response.SpuDetailResponse;
import com.chareslm.shopping.product.dto.response.SpuResponse;
import com.chareslm.shopping.product.entity.ProductStatusLog;
import com.chareslm.shopping.product.entity.Sku;
import com.chareslm.shopping.product.entity.Spu;
import com.chareslm.shopping.product.enums.SpuStatus;
import com.chareslm.shopping.product.event.ProductChangedEvent;
import com.chareslm.shopping.product.mapper.ProductStatusLogMapper;
import com.chareslm.shopping.product.mapper.SkuMapper;
import com.chareslm.shopping.product.mapper.SpuMapper;
import com.chareslm.shopping.product.service.SpuService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpuServiceImpl implements SpuService {

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final ProductStatusLogMapper productStatusLogMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SpuDetailResponse create(Long operatorId, SpuCreateRequest request) {
        Spu spu = new Spu();
        spu.setShopId(request.shopId());
        spu.setCategoryId(request.categoryId());
        spu.setBrand(trimToNull(request.brand()));
        spu.setName(request.name().trim());
        spu.setSubtitle(trimToNull(request.subtitle()));
        spu.setMainImage(trimToNull(request.mainImage()));
        spu.setImages(toJson(request.images()));
        spu.setDetail(request.detail());
        spu.setSales(0);
        spu.setRating(BigDecimal.ZERO);
        spu.setStatus(SpuStatus.DRAFT.name());
        spuMapper.insert(spu);

        for (SkuCreateRequest skuRequest : request.skus()) {
            insertSku(spu, skuRequest);
        }
        refreshPriceRange(spu);
        eventPublisher.publishEvent(new ProductChangedEvent(spu.getId(), spu.getStatus()));
        return toDetail(spu);
    }

    @Override
    @Transactional
    public SpuDetailResponse update(Long operatorId, Long spuId, SpuUpdateRequest request) {
        Spu spu = requireSpu(spuId);
        SpuStatus current = SpuStatus.valueOf(spu.getStatus());
        spu.setCategoryId(request.categoryId());
        spu.setBrand(trimToNull(request.brand()));
        spu.setName(request.name().trim());
        spu.setSubtitle(trimToNull(request.subtitle()));
        spu.setMainImage(trimToNull(request.mainImage()));
        spu.setImages(toJson(request.images()));
        spu.setDetail(request.detail());
        // 修改受审字段使原审核结果失效：已上架/待上架商品回到草稿，需重新提交审核
        if (current == SpuStatus.ON_SALE || current == SpuStatus.OFF_SALE
                || current == SpuStatus.AUDIT_APPROVED) {
            spu.setStatus(SpuStatus.DRAFT.name());
        }
        spuMapper.updateById(spu);
        eventPublisher.publishEvent(new ProductChangedEvent(spu.getId(), spu.getStatus()));
        return toDetail(spu);
    }

    @Override
    public SpuDetailResponse getDetail(Long spuId) {
        return toDetail(requireSpu(spuId));
    }

    @Override
    public PageResponse<SpuResponse> page(Long categoryId, String keyword, String status, int page, int pageSize) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<Spu>()
                .eq(categoryId != null, Spu::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Spu::getName, keyword)
                .eq(StringUtils.hasText(status), Spu::getStatus, status)
                .orderByDesc(Spu::getCreatedAt);
        Page<Spu> result = spuMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResponse<>(result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(), page, pageSize);
    }

    @Override
    public PageResponse<SpuResponse> search(Long categoryId, String keyword, String brand,
                                            BigDecimal priceMin, BigDecimal priceMax,
                                            String sort, int page, int pageSize) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<Spu>()
                .eq(Spu::getStatus, "ON_SALE")
                .eq(categoryId != null, Spu::getCategoryId, categoryId)
                .eq(StringUtils.hasText(brand), Spu::getBrand, brand)
                .ge(priceMin != null, Spu::getPriceMin, priceMin)
                .le(priceMax != null, Spu::getPriceMax, priceMax)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Spu::getName, keyword)
                        .or().like(Spu::getSubtitle, keyword)
                        .or().like(Spu::getBrand, keyword));
        applySearchSort(wrapper, sort);
        Page<Spu> result = spuMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResponse<>(result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(), page, pageSize);
    }

    private void applySearchSort(LambdaQueryWrapper<Spu> wrapper, String sort) {
        if (sort == null || sort.isBlank() || "DEFAULT".equals(sort)) {
            wrapper.orderByDesc(Spu::getSales).orderByDesc(Spu::getCreatedAt);
            return;
        }
        switch (sort) {
            case "SALES_DESC" -> wrapper.orderByDesc(Spu::getSales);
            case "PRICE_ASC" -> wrapper.orderByAsc(Spu::getPriceMin);
            case "PRICE_DESC" -> wrapper.orderByDesc(Spu::getPriceMax);
            case "RATING_DESC" -> wrapper.orderByDesc(Spu::getRating);
            case "NEWEST" -> wrapper.orderByDesc(Spu::getCreatedAt);
            default -> wrapper.orderByDesc(Spu::getSales);
        }
    }

    @Override
    @Transactional
    public SpuResponse changeStatus(Long operatorId, Long spuId, SpuStatusRequest request) {
        Spu spu = requireSpu(spuId);
        SpuStatus current = SpuStatus.valueOf(spu.getStatus());
        SpuStatus target = switch (request.action()) {
            case "SUBMIT" -> SpuStatus.PENDING_AUDIT;
            case "PUBLISH" -> SpuStatus.ON_SALE;
            case "OFF_SHELF" -> SpuStatus.OFF_SALE;
            default -> throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID);
        };
        if (!current.canTransitionTo(target)) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID);
        }
        transition(spu, current, target, operatorId, request.action(), request.remark());
        return toResponse(spu);
    }

    @Override
    @Transactional
    public SpuResponse audit(Long operatorId, Long spuId, SpuAuditRequest request) {
        Spu spu = requireSpu(spuId);
        SpuStatus current = SpuStatus.valueOf(spu.getStatus());
        if (current != SpuStatus.PENDING_AUDIT) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID);
        }
        SpuStatus target = "APPROVE".equals(request.result()) ? SpuStatus.AUDIT_APPROVED : SpuStatus.AUDIT_REJECTED;
        String action = "APPROVE".equals(request.result()) ? "APPROVE" : "REJECT";
        spu.setAuditRemark(request.remark());
        transition(spu, current, target, operatorId, action, request.remark());
        return toResponse(spu);
    }

    @Override
    @Transactional
    public SpuDetailResponse addSku(Long operatorId, Long spuId, SkuCreateRequest request) {
        Spu spu = requireSpu(spuId);
        insertSku(spu, request);
        refreshPriceRange(spu);
        return toDetail(spu);
    }

    @Override
    public void updateRating(Long spuId, BigDecimal rating) {
        spuMapper.updateRating(spuId, rating);
    }

    @Override
    public List<SpuResponse> listByStatus(String status) {
        return spuMapper.selectList(new LambdaQueryWrapper<Spu>().eq(Spu::getStatus, status))
                .stream().map(this::toResponse).toList();
    }

    @Override
    public SpuResponse getSpu(Long spuId) {
        return toResponse(requireSpu(spuId));
    }

    private void transition(Spu spu, SpuStatus from, SpuStatus to, Long operatorId, String action, String remark) {
        spu.setStatus(to.name());
        spuMapper.updateById(spu);
        ProductStatusLog log = new ProductStatusLog();
        log.setSpuId(spu.getId());
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setFromStatus(from.name());
        log.setToStatus(to.name());
        log.setRemark(remark);
        productStatusLogMapper.insert(log);
        eventPublisher.publishEvent(new ProductChangedEvent(spu.getId(), to.name()));
    }

    private void insertSku(Spu spu, SkuCreateRequest request) {
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        sku.setSkuCode(trimToNull(request.skuCode()));
        sku.setAttributes(trimToNull(request.attributes()));
        sku.setImage(trimToNull(request.image()));
        sku.setPrice(request.price());
        sku.setAvailableStock(request.stock().intValue());
        sku.setReservedStock(0);
        sku.setSoldStock(0);
        sku.setStatus(1);
        skuMapper.insert(sku);
    }

    private void refreshPriceRange(Spu spu) {
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spu.getId()));
        if (skus.isEmpty()) {
            spu.setPriceMin(null);
            spu.setPriceMax(null);
        } else {
            BigDecimal min = skus.stream().map(Sku::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal max = skus.stream().map(Sku::getPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            spu.setPriceMin(min);
            spu.setPriceMax(max);
        }
        spuMapper.updateById(spu);
    }

    private Spu requireSpu(Long spuId) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return spu;
    }

    private SpuDetailResponse toDetail(Spu spu) {
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spu.getId()));
        return new SpuDetailResponse(
                spu.getId(), spu.getShopId(), spu.getCategoryId(), spu.getBrand(), spu.getName(),
                spu.getSubtitle(), spu.getMainImage(), fromJson(spu.getImages()), spu.getDetail(),
                spu.getPriceMin(), spu.getPriceMax(), spu.getSales(), spu.getRating(), spu.getStatus(),
                spu.getAuditRemark(), spu.getCreatedAt(),
                skus.stream().map(SpuServiceImpl::toSkuResponse).toList());
    }

    private SpuResponse toResponse(Spu spu) {
        return new SpuResponse(spu.getId(), spu.getShopId(), spu.getCategoryId(), spu.getBrand(), spu.getName(),
                spu.getSubtitle(), spu.getMainImage(), spu.getPriceMin(), spu.getPriceMax(),
                spu.getSales(), spu.getRating(), spu.getStatus());
    }

    private static SkuResponse toSkuResponse(Sku sku) {
        return new SkuResponse(sku.getId(), sku.getSpuId(), sku.getSkuCode(), sku.getAttributes(), sku.getImage(),
                sku.getPrice(), sku.getAvailableStock(), sku.getReservedStock(), sku.getSoldStock(), sku.getStatus());
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("serialize images failed", e);
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("deserialize images failed", e);
            return List.of();
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
