package com.chareslm.shopping.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.product.entity.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface SkuMapper extends BaseMapper<Sku> {

    /**
     * 原子预占库存：可售充足则 available_stock 减、reserved_stock 增。
     * 使用单条条件 UPDATE，避免"先查再改"的并发竞态。影响行数为 0 表示库存不足。
     */
    @Update("""
            UPDATE sku
            SET available_stock = available_stock - #{quantity},
                reserved_stock = reserved_stock + #{quantity}
            WHERE id = #{skuId}
              AND status = 1
              AND available_stock >= #{quantity}
            """)
    int reserveStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    /**
     * 原子释放预占：reserved_stock 减、available_stock 增（超时关单回补）。
     */
    @Update("""
            UPDATE sku
            SET reserved_stock = GREATEST(reserved_stock - #{quantity}, 0),
                available_stock = available_stock + #{quantity}
            WHERE id = #{skuId}
            """)
    int releaseStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    /**
     * 原子扣减库存：支付成功后把预占转为已售（reserved_stock 减、sold_stock 增）。
     * 注意：available_stock 已在预占时扣减，此处不再重复扣减。
     */
    @Update("""
            UPDATE sku
            SET reserved_stock = reserved_stock - #{quantity},
                sold_stock = sold_stock + #{quantity}
            WHERE id = #{skuId}
              AND reserved_stock >= #{quantity}
            """)
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    /**
     * 增加可售库存（商家补货）。
     */
    @Update("""
            UPDATE sku
            SET available_stock = available_stock + #{quantity}
            WHERE id = #{skuId}
            """)
    int increaseAvailableStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    /**
     * 减少可售库存（商家扣减，不得使可售为负）。
     */
    @Update("""
            UPDATE sku
            SET available_stock = available_stock - #{quantity}
            WHERE id = #{skuId}
              AND available_stock >= #{quantity}
            """)
    int reduceAvailableStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}
