package com.chareslm.shopping.cart.controller;

import com.chareslm.shopping.cart.dto.CartDTO;
import com.chareslm.shopping.cart.dto.request.AddCartItemRequest;
import com.chareslm.shopping.cart.dto.request.UpdateCheckedRequest;
import com.chareslm.shopping.cart.dto.request.UpdateQuantityRequest;
import com.chareslm.shopping.cart.service.CartService;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物车接口（用户端）。
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartDTO> getCart() {
        return ApiResponse.success(cartService.getCart(CurrentUser.require().userId()));
    }

    @PostMapping("/items")
    public ApiResponse<Void> addItem(@Valid @RequestBody AddCartItemRequest request) {
        cartService.addItem(CurrentUser.require().userId(), request.getSkuId(),
                request.getQuantity(), request.getShopId(), request.getPrice());
        return ApiResponse.success(null);
    }

    @PutMapping("/items/{itemId}/quantity")
    public ApiResponse<Void> updateQuantity(@PathVariable Long itemId,
                                            @Valid @RequestBody UpdateQuantityRequest request) {
        cartService.updateQuantity(CurrentUser.require().userId(), itemId, request.getQuantity());
        return ApiResponse.success(null);
    }

    @PutMapping("/items/{itemId}/checked")
    public ApiResponse<Void> updateChecked(@PathVariable Long itemId,
                                           @Valid @RequestBody UpdateCheckedRequest request) {
        cartService.updateChecked(CurrentUser.require().userId(), itemId, request.getChecked());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> removeItem(@PathVariable Long itemId) {
        cartService.removeItem(CurrentUser.require().userId(), itemId);
        return ApiResponse.success(null);
    }
}