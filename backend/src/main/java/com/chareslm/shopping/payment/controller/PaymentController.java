package com.chareslm.shopping.payment.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.payment.dto.CreatePaymentRequest;
import com.chareslm.shopping.payment.dto.PaymentOrderDTO;
import com.chareslm.shopping.payment.service.PaymentService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付接口（用户端）。
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<PaymentOrderDTO> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.success(paymentService.createPaymentOrder(CurrentUser.require().userId(), request));
    }

    @PostMapping("/{paymentOrderId}/mock-pay")
    public ApiResponse<PaymentOrderDTO> mockPay(@PathVariable Long paymentOrderId) {
        return ApiResponse.success(paymentService.mockPay(CurrentUser.require().userId(), paymentOrderId));
    }

    @GetMapping("/{paymentOrderId}")
    public ApiResponse<PaymentOrderDTO> getPayment(@PathVariable Long paymentOrderId) {
        return ApiResponse.success(paymentService.getPaymentOrder(CurrentUser.require().userId(), paymentOrderId));
    }
}