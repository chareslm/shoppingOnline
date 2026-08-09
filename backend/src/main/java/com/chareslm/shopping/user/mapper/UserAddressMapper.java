package com.chareslm.shopping.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.user.entity.UserAddress;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserAddressMapper extends BaseMapper<UserAddress> {
    @Select("""
            SELECT * FROM user_address
            WHERE user_id = #{userId}
            ORDER BY is_default DESC, updated_at DESC, id DESC
            """)
    List<UserAddress> selectByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT * FROM user_address
            WHERE id = #{addressId} AND user_id = #{userId}
            """)
    UserAddress selectByIdAndUserId(@Param("addressId") Long addressId, @Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*) FROM user_address
            WHERE user_id = #{userId}
            """)
    long countByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE user_address SET is_default = 0
            WHERE user_id = #{userId} AND is_default = 1
            """)
    int clearDefaultByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE user_address SET is_default = 1
            WHERE id = #{addressId} AND user_id = #{userId}
            """)
    int markDefault(@Param("addressId") Long addressId, @Param("userId") Long userId);
}
