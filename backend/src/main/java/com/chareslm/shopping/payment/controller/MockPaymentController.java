package com.chareslm.shopping.payment.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.payment.dto.PaymentOrderDTO;
import com.chareslm.shopping.payment.service.PaymentService;
import com.chareslm.shopping.security.context.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Development-only payment completion endpoints; disabled unless explicitly enabled. */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "trade.payment", name = "mock-enabled", havingValue = "true")
public class MockPaymentController {
    private final PaymentService paymentService;

    @PostMapping("/api/payments/{paymentOrderId}/mock-pay")
    public ApiResponse<PaymentOrderDTO> mockPay(@PathVariable Long paymentOrderId) {
        return ApiResponse.success(paymentService.mockPay(CurrentUser.require().userId(), paymentOrderId));
    }

    @PostMapping("/api/refunds/{refundId}/mock-complete")
    public ApiResponse<Void> mockComplete(@PathVariable Long refundId) {
        paymentService.mockCompleteRefund(CurrentUser.require().userId(), refundId);
        return ApiResponse.success(null);
    }
}
