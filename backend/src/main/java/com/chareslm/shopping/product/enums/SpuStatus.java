package com.chareslm.shopping.product.enums;

/**
 * 商品 SPU 状态机。
 * <p>
 * 商家：DRAFT / AUDIT_REJECTED → PENDING_AUDIT →（管理员通过）ON_SALE；
 * ON_SALE → OFF_SALE → ON_SALE。管理员可收回已通过商品至 AUDIT_REJECTED，并可再次 APPROVE 上架。
 */
public enum SpuStatus {
    DRAFT,
    PENDING_AUDIT,
    AUDIT_APPROVED,
    AUDIT_REJECTED,
    ON_SALE,
    OFF_SALE;

    /**
     * 商家自发状态流转是否合法。管理员审核结论不走该方法。
     */
    public boolean canTransitionTo(SpuStatus target) {
        return switch (this) {
            case DRAFT, AUDIT_REJECTED -> target == PENDING_AUDIT;
            case PENDING_AUDIT -> false;
            case AUDIT_APPROVED -> target == ON_SALE || target == PENDING_AUDIT;
            case ON_SALE -> target == OFF_SALE;
            case OFF_SALE -> target == ON_SALE;
        };
    }
}
