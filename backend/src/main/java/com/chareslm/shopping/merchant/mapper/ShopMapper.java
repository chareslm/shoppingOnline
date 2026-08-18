package com.chareslm.shopping.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.merchant.entity.Shop;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 审核开通阶段使用的店铺持久化入口。
 */
public interface ShopMapper extends BaseMapper<Shop> {
    @Select("SELECT * FROM shop WHERE application_id = #{applicationId}")
    Shop selectByApplicationId(@Param("applicationId") Long applicationId);

    @Update("UPDATE shop SET status = #{toStatus} WHERE id = #{id} AND status = #{fromStatus}")
    int updateStatus(@Param("id") Long id, @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);
}
