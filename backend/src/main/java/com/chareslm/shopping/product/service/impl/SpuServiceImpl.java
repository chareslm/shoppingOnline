package com.chareslm.shopping.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.mapper.ShopMapper;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import com.chareslm.shopping.product.dto.request.SkuCreateRequest;
import com.chareslm.shopping.product.dto.request.SpuAuditRequest;
import com.chareslm.shopping.product.dto.request.SpuCreateRequest;
import com.chareslm.shopping.product.dto.request.SpuStatusRequest;
import com.chareslm.shopping.product.dto.request.SpuUpdateRequest;
import com.chareslm.shopping.product.dto.response.SkuResponse;
import com.chareslm.shopping.product.dto.response.SpuDetailResponse;
import com.chareslm.shopping.product.dto.response.SpuResponse;
import com.chareslm.shopping.product.entity.Category;
import com.chareslm.shopping.product.entity.ProductStatusLog;
import com.chareslm.shopping.product.entity.Sku;
import com.chareslm.shopping.product.entity.Spu;
import com.chareslm.shopping.product.enums.SpuStatus;
import com.chareslm.shopping.product.event.ProductChangedEvent;
import com.chareslm.shopping.product.mapper.CategoryMapper;
import com.chareslm.shopping.product.mapper.ProductStatusLogMapper;
import com.chareslm.shopping.product.mapper.SkuMapper;
import com.chareslm.shopping.product.mapper.SpuMapper;
import com.chareslm.shopping.product.service.SpuService;
import com.chareslm.shopping.product.util.SkuAttributes;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpuServiceImpl implements SpuService {

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final ProductStatusLogMapper productStatusLogMapper;
    private final CategoryMapper categoryMapper;
    private final ShopMapper shopMapper;
    private final MerchantShopQueryService merchantShopQueryService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SpuDetailResponse create(Long operatorId, SpuCreateRequest request) {
        Shop shop = merchantShopQueryService.requireOpenShop(operatorId);
        Spu spu = new Spu();
        spu.setShopId(shop.getId());
        spu.setCategoryId(parseId(request.categoryId()));
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
        Spu spu = requireOwnedSpu(operatorId, spuId);
        SpuStatus current = SpuStatus.valueOf(spu.getStatus());
        spu.setCategoryId(parseId(request.categoryId()));
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
    public SpuDetailResponse getOwnedDetail(Long operatorId, Long spuId) {
        return toDetail(requireOwnedSpu(operatorId, spuId));
    }

    @Override
    public PageResponse<SpuResponse> page(Long categoryId, String keyword, String status, int page, int pageSize) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<Spu>()
                .eq(categoryId != null, Spu::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Spu::getName, keyword)
                .eq(StringUtils.hasText(status), Spu::getStatus, status)
                .orderByDesc(Spu::getCreatedAt);
        Page<Spu> result = spuMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return toPage(result, page, pageSize);
    }

    @Override
    public PageResponse<SpuResponse> pageForShop(Long shopId, Long categoryId, String keyword, String status,
                                                 String shelf, int page, int pageSize) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<Spu>()
                .eq(Spu::getShopId, shopId)
                .eq(categoryId != null, Spu::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Spu::getName, keyword)
                .eq(StringUtils.hasText(status), Spu::getStatus, status)
                .orderByDesc(Spu::getCreatedAt);
        if ("LISTED".equals(shelf)) {
            wrapper.eq(Spu::getStatus, SpuStatus.ON_SALE.name());
        } else if ("UNLISTED".equals(shelf)) {
            wrapper.ne(Spu::getStatus, SpuStatus.ON_SALE.name());
        }
        Page<Spu> result = spuMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return toPage(result, page, pageSize);
    }

    @Override
    public PageResponse<SpuResponse> pageForMerchant(Long operatorId, Long categoryId, String keyword, String status,
                                                     String shelf, int page, int pageSize) {
        Long shopId = merchantShopQueryService.requireOpenShop(operatorId).getId();
        return pageForShop(shopId, categoryId, keyword, status, shelf, page, pageSize);
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
        return toPage(result, page, pageSize);
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
        Spu spu = requireOwnedSpu(operatorId, spuId);
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
        SpuStatus target = switch (request.result()) {
            case "APPROVE" -> {
                if (current != SpuStatus.PENDING_AUDIT && current != SpuStatus.AUDIT_REJECTED
                        && current != SpuStatus.AUDIT_APPROVED) {
                    throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID);
                }
                yield SpuStatus.ON_SALE;
            }
            case "REJECT" -> {
                if (current != SpuStatus.PENDING_AUDIT) {
                    throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID);
                }
                yield SpuStatus.AUDIT_REJECTED;
            }
            case "REVOKE" -> {
                if (current != SpuStatus.ON_SALE && current != SpuStatus.AUDIT_APPROVED
                        && current != SpuStatus.OFF_SALE) {
                    throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID);
                }
                yield SpuStatus.AUDIT_REJECTED;
            }
            default -> throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID);
        };
        String action = request.result();
        spu.setAuditRemark(request.remark());
        transition(spu, current, target, operatorId, action, request.remark());
        return toResponse(spu);
    }

    @Override
    @Transactional
    public SpuDetailResponse addSku(Long operatorId, Long spuId, SkuCreateRequest request) {
        Spu spu = requireOwnedSpu(operatorId, spuId);
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
        sku.setAttributes(SkuAttributes.normalize(request.attributes()));
        sku.setImage(trimToNull(request.image()));
        sku.setPrice(request.price());
        sku.setAvailableStock(request.stock().intValue());
        sku.setReservedStock(0);
        sku.setSoldStock(0);
        sku.setStatus(1);
        try {
            skuMapper.insert(sku);
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.SKU_CODE_DUPLICATE.code(),
                    "SKU 编码与已有商品冲突，请修改编码后重试");
        }
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

    private Spu requireOwnedSpu(Long operatorId, Long spuId) {
        Shop shop = merchantShopQueryService.requireOpenShop(operatorId);
        Spu spu = requireSpu(spuId);
        if (!shop.getId().equals(spu.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return spu;
    }

    private SpuDetailResponse toDetail(Spu spu) {
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spu.getId()));
        Shop shop = spu.getShopId() == null ? null : shopMapper.selectById(spu.getShopId());
        Category category = spu.getCategoryId() == null ? null : categoryMapper.selectById(spu.getCategoryId());
        return new SpuDetailResponse(
                spu.getId(), spu.getShopId(), shop == null ? null : shop.getName(),
                spu.getCategoryId(), category == null ? null : category.getName(),
                spu.getBrand(), spu.getName(),
                spu.getSubtitle(), spu.getMainImage(), fromJson(spu.getImages()), spu.getDetail(),
                spu.getPriceMin(), spu.getPriceMax(), spu.getSales(), spu.getRating(), spu.getStatus(),
                spu.getAuditRemark(), spu.getCreatedAt(),
                skus.stream().map(SpuServiceImpl::toSkuResponse).toList());
    }

    private PageResponse<SpuResponse> toPage(Page<Spu> result, int page, int pageSize) {
        List<Spu> records = result.getRecords();
        List<Long> shopIds = records.stream().map(Spu::getShopId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> shopNames = shopIds.isEmpty()
                ? Map.of()
                : shopMapper.selectByIds(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getName, (left, right) -> left));
        return new PageResponse<>(records.stream()
                .map(spu -> toResponse(spu, shopNames.get(spu.getShopId())))
                .toList(), result.getTotal(), page, pageSize);
    }

    private SpuResponse toResponse(Spu spu) {
        Shop shop = spu.getShopId() == null ? null : shopMapper.selectById(spu.getShopId());
        return toResponse(spu, shop == null ? null : shop.getName());
    }

    private static SpuResponse toResponse(Spu spu, String shopName) {
        return new SpuResponse(spu.getId(), spu.getShopId(), shopName,
                spu.getCategoryId(), spu.getBrand(), spu.getName(),
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

    private static Long parseId(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
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
