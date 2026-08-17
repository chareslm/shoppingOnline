package com.chareslm.shopping.product.enums;

/**
 * 商品 SPU 状态机。
 * <p>
 * 流转：DRAFT → PENDING_AUDIT → AUDIT_APPROVED → ON_SALE → OFF_SALE → ON_SALE；
 * PENDING_AUDIT 可驳回至 AUDIT_REJECTED，AUDIT_REJECTED/DRAFT 可重新提交审核。
 */
public enum SpuStatus {
    DRAFT,
    PENDING_AUDIT,
    AUDIT_APPROVED,
    AUDIT_REJECTED,
    ON_SALE,
    OFF_SALE;

    /**
     * 是否允许从当前状态流转到目标状态。
     */
    public boolean canTransitionTo(SpuStatus target) {
        return switch (this) {
            case DRAFT, AUDIT_REJECTED -> target == PENDING_AUDIT;
            case PENDING_AUDIT -> target == AUDIT_APPROVED || target == AUDIT_REJECTED;
            case AUDIT_APPROVED -> target == ON_SALE || target == PENDING_AUDIT;
            case ON_SALE -> target == OFF_SALE;
            case OFF_SALE -> target == ON_SALE || target == PENDING_AUDIT;
        };
    }
}
