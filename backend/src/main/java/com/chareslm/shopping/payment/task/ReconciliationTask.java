package com.chareslm.shopping.payment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chareslm.shopping.payment.entity.PaymentOrder;
import com.chareslm.shopping.payment.entity.ReconciliationRecord;
import com.chareslm.shopping.payment.mapper.PaymentOrderMapper;
import com.chareslm.shopping.payment.mapper.ReconciliationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日对账定时任务（设计文档 §5.5）。
 * <p>
 * 每日 02:00 汇总前一日成功支付单（金额/笔数），与渠道侧对比写入 reconciliation_record。
 * 本地模拟：渠道侧数据与本地一致（diff=0, status=1）。
 */
@Component
@RequiredArgsConstructor
public class ReconciliationTask {

    private static final String CHANNEL = "MOCK_WECHAT";

    private final PaymentOrderMapper paymentOrderMapper;
    private final ReconciliationRecordMapper reconciliationRecordMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void reconcileDaily() {
        LocalDate bizDate = LocalDate.now().minusDays(1);
        LocalDateTime start = bizDate.atStartOfDay();
        LocalDateTime end = bizDate.plusDays(1).atStartOfDay();
        List<PaymentOrder> paid = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getStatus, 1)
                .ge(PaymentOrder::getPayTime, start)
                .lt(PaymentOrder::getPayTime, end));
        BigDecimal totalAmount = paid.stream()
                .map(PaymentOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReconciliationRecord record = new ReconciliationRecord();
        record.setBizDate(bizDate);
        record.setChannel(CHANNEL);
        record.setTotalAmount(totalAmount);
        record.setTotalCount(paid.size());
        record.setDiffCount(0);
        record.setDiffAmount(BigDecimal.ZERO);
        record.setStatus(1);
        reconciliationRecordMapper.insert(record);
    }
}