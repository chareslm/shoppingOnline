package com.chareslm.shopping.search.listener;

import com.chareslm.shopping.product.event.ProductChangedEvent;
import com.chareslm.shopping.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 商品变更事件监听：事务提交后同步 Elasticsearch 索引（最终一致）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductChangedListener {

    private final SearchIndexService searchIndexService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductChanged(ProductChangedEvent event) {
        try {
            searchIndexService.index(event.spuId());
        } catch (Exception e) {
            log.warn("sync ES index on product change failed, spuId={}", event.spuId(), e);
        }
    }
}
