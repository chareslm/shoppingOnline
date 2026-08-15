package com.chareslm.shopping.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.product.entity.Spu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface SpuMapper extends BaseMapper<Spu> {

    /**
     * 原子累加销量（支付成功回写）。
     */
    @Update("""
            UPDATE spu SET sales = sales + #{quantity}
            WHERE id = #{spuId}
            """)
    int increaseSales(@Param("spuId") Long spuId, @Param("quantity") int quantity);

    /**
     * 回写平均评分（评价模块调用）。
     */
    @Update("""
            UPDATE spu SET rating = #{rating}
            WHERE id = #{spuId}
            """)
    int updateRating(@Param("spuId") Long spuId, @Param("rating") java.math.BigDecimal rating);
}
