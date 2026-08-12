package com.chareslm.shopping.payment.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.payment.service.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 模拟微信支付回调接口（公开，无 JWT）。
 * <p>
 * 真实场景由微信服务器回调；本地开发用此接口模拟 PAY / REFUND 回调，
 * 走与真实回调完全相同的幂等处理逻辑。
 */
@RestController
@RequestMapping("/api/mock/wechat/callback")
public class MockWechatCallbackController {

    private final PaymentService paymentService;

    public MockWechatCallbackController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/pay")
    public ApiResponse<Void> payCallback(@RequestBody Map<String, Object> body) {
        Long paymentOrderId = Long.valueOf(String.valueOf(body.get("paymentOrderId")));
        String rawData = body.get("rawData") == null ? "{}" : String.valueOf(body.get("rawData"));
        paymentService.handlePayCallback(paymentOrderId, rawData);
        return ApiResponse.success(null);
    }

    @PostMapping("/refund")
    public ApiResponse<Void> refundCallback(@RequestBody Map<String, Object> body) {
        Long refundId = Long.valueOf(String.valueOf(body.get("refundId")));
        paymentService.completeRefund(refundId);
        return ApiResponse.success(null);
    }
}