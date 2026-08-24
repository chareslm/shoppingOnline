package com.chareslm.shopping.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chareslm.shopping.cart.client.ProductQueryClient;
import com.chareslm.shopping.cart.dto.CartDTO;
import com.chareslm.shopping.cart.dto.CartGroupDTO;
import com.chareslm.shopping.cart.dto.CartItemDTO;
import com.chareslm.shopping.cart.dto.ProductSkuView;
import com.chareslm.shopping.cart.entity.Cart;
import com.chareslm.shopping.cart.entity.CartGroup;
import com.chareslm.shopping.cart.entity.CartItem;
import com.chareslm.shopping.cart.mapper.CartGroupMapper;
import com.chareslm.shopping.cart.mapper.CartItemMapper;
import com.chareslm.shopping.cart.mapper.CartMapper;
import com.chareslm.shopping.cart.service.CartService;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车服务实现。
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final CartGroupMapper cartGroupMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductQueryClient productQueryClient;

    @Override
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        Cart cart = cartMapper.selectOne(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setStatus(1);
            cartMapper.insert(cart);
        }
        return cart;
    }

    @Override
    @Transactional
    public void addItem(Long userId, Long skuId, int quantity, Long shopId, BigDecimal price) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        // 商品有效性校验：SKU 存在且启用、所属 SPU 上架；价格以服务端为准，不信任客户端提交价
        ProductSkuView sku = productQueryClient.getSkuSnapshot(skuId);
        if (sku == null || !sku.onSale()) {
            throw new BusinessException(ErrorCode.SKU_NOT_AVAILABLE);
        }
        if (!sku.shopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        BigDecimal serverPrice = sku.price();
        Cart cart = getOrCreateCart(userId);
        CartGroup group = cartGroupMapper.selectOne(new LambdaQueryWrapper<CartGroup>()
                .eq(CartGroup::getCartId, cart.getId())
                .eq(CartGroup::getShopId, shopId));
        if (group == null) {
            group = new CartGroup();
            group.setCartId(cart.getId());
            group.setShopId(shopId);
            group.setStatus(1);
            cartGroupMapper.insert(group);
        }
        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getCartId, cart.getId())
                .eq(CartItem::getSkuId, skuId));
        if (item != null) {
            item.setQuantity(item.getQuantity() + quantity);
            item.setPriceSnapshot(serverPrice);
            item.setStatus(1);
            cartItemMapper.updateById(item);
        } else {
            item = new CartItem();
            item.setCartId(cart.getId());
            item.setGroupId(group.getId());
            item.setSkuId(skuId);
            item.setQuantity(quantity);
            item.setPriceSnapshot(serverPrice);
            item.setChecked(1);
            item.setStatus(1);
            cartItemMapper.insert(item);
        }
    }

    @Override
    @Transactional
    public void updateQuantity(Long userId, Long itemId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        CartItem item = requireOwnedItem(userId, itemId);
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void updateChecked(Long userId, Long itemId, int checked) {
        CartItem item = requireOwnedItem(userId, itemId);
        item.setChecked(checked);
        cartItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long itemId) {
        CartItem item = requireOwnedItem(userId, itemId);
        item.setStatus(0);
        cartItemMapper.updateById(item);
    }

    @Override
    public CartDTO getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        CartDTO dto = new CartDTO();
        dto.setCartId(cart.getId());
        List<CartGroupDTO> groupDTOs = new ArrayList<>();
        List<CartGroup> groups = cartGroupMapper.selectList(new LambdaQueryWrapper<CartGroup>()
                .eq(CartGroup::getCartId, cart.getId())
                .eq(CartGroup::getStatus, 1));
        for (CartGroup group : groups) {
            CartGroupDTO groupDTO = new CartGroupDTO();
            groupDTO.setGroupId(group.getId());
            groupDTO.setShopId(group.getShopId());
            List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getGroupId, group.getId())
                    .eq(CartItem::getStatus, 1));
            groupDTO.setItems(items.stream().map(this::toItemDTO).toList());
            groupDTOs.add(groupDTO);
        }
        dto.setGroups(groupDTOs);
        return dto;
    }

    private CartItem requireOwnedItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !item.getCartId().equals(cart.getId()) || item.getStatus() == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return item;
    }

    private CartItemDTO toItemDTO(CartItem item) {
        CartItemDTO dto = new CartItemDTO();
        dto.setItemId(item.getId());
        dto.setSkuId(item.getSkuId());
        ProductSkuView sku = productQueryClient.getSkuSnapshot(item.getSkuId());
        if (sku != null) {
            dto.setSkuName(sku.skuName());
            dto.setSkuImage(sku.skuImage());
        }
        dto.setPrice(item.getPriceSnapshot());
        dto.setQuantity(item.getQuantity());
        dto.setChecked(item.getChecked());
        dto.setGroupId(item.getGroupId());
        return dto;
    }
}