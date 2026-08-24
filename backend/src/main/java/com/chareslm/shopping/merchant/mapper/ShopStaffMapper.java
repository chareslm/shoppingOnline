package com.chareslm.shopping.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.merchant.entity.ShopStaff;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ShopStaffMapper extends BaseMapper<ShopStaff> {
    @Select("""
            SELECT ss.*, s.name AS shop_name
            FROM shop_staff ss
            LEFT JOIN shop s ON s.id = ss.shop_id
            WHERE ss.shop_id = #{shopId}
            ORDER BY ss.id DESC
            """)
    List<ShopStaff> selectByShopId(@Param("shopId") Long shopId);

    @Select("""
            SELECT ss.*, s.name AS shop_name
            FROM shop_staff ss
            LEFT JOIN shop s ON s.id = ss.shop_id
            WHERE ss.id = #{id} AND ss.shop_id = #{shopId}
            """)
    ShopStaff selectByIdAndShopId(@Param("id") Long id, @Param("shopId") Long shopId);

    @Select("""
            SELECT ss.*, s.name AS shop_name
            FROM shop_staff ss
            LEFT JOIN shop s ON s.id = ss.shop_id
            WHERE (#{status} IS NULL OR #{status} = '' OR ss.status = #{status})
            ORDER BY ss.id DESC
            """)
    List<ShopStaff> selectByStatus(@Param("status") String status);

    @Select("""
            SELECT ss.*, s.name AS shop_name
            FROM shop_staff ss
            LEFT JOIN shop s ON s.id = ss.shop_id
            WHERE ss.user_id = #{userId} AND ss.status = 'ACTIVE'
            """)
    ShopStaff selectActiveByUserId(@Param("userId") Long userId);

    @Update("UPDATE shop_staff SET email_delivery_status = #{status} WHERE id = #{id}")
    int updateEmailStatus(@Param("id") Long id, @Param("status") String status);
}
