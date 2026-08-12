package com.chareslm.shopping.trade.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import com.chareslm.shopping.trade.dto.CreateOrderRequest;
import com.chareslm.shopping.trade.dto.OrderDTO;
import com.chareslm.shopping.trade.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口（用户端）。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<List<OrderDTO>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrder(CurrentUser.require().userId(), request));
    }

    @GetMapping
    public ApiResponse<List<OrderDTO>> listMyOrders() {
        return ApiResponse.success(orderService.getMyOrders(CurrentUser.require().userId()));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDTO> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrder(CurrentUser.require().userId(), orderId));
    }

    @PutMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(CurrentUser.require().userId(), orderId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{orderId}/confirm")
    public ApiResponse<Void> confirmReceipt(@PathVariable Long orderId) {
        orderService.confirmReceipt(CurrentUser.require().userId(), orderId);
        return ApiResponse.success(null);
    }
}