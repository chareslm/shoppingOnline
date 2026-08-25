package com.chareslm.shopping.payment.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.payment.dto.RefundOrderDTO;
import com.chareslm.shopping.payment.dto.RefundRequest;
import com.chareslm.shopping.payment.service.PaymentService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 退款接口（用户端）。
 */
@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final PaymentService paymentService;

    public RefundController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<Void> refund(@Valid @RequestBody RefundRequest request) {
        paymentService.refund(CurrentUser.require().userId(), request);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<RefundOrderDTO>> listRefunds() {
        return ApiResponse.success(paymentService.listRefunds(CurrentUser.require().userId()));
    }

}
