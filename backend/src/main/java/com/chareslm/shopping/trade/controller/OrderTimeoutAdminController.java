package com.chareslm.shopping.trade.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.trade.task.OrderTimeoutTask;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 超时任务手动触发接口（管理端/运维调试）。
 * <p>
 * 定时任务默认每分钟自动执行；此接口用于演示或手动补跑。
 */
@RestController
@RequestMapping("/api/admin/tasks")
public class OrderTimeoutAdminController {

    private final OrderTimeoutTask orderTimeoutTask;

    public OrderTimeoutAdminController(OrderTimeoutTask orderTimeoutTask) {
        this.orderTimeoutTask = orderTimeoutTask;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/order-timeout")
    public ApiResponse<Map<String, Integer>> runOrderTimeout() {
        int closed = orderTimeoutTask.closeExpiredOrders();
        return ApiResponse.success(Map.of("closedCount", closed));
    }
}