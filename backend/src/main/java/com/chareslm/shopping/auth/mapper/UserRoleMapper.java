package com.chareslm.shopping.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.auth.entity.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface UserRoleMapper extends BaseMapper<UserRole> {
    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("""
            DELETE ur FROM user_role ur
            INNER JOIN `role` r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.code IN ('USER', 'ADMIN', 'SUPER_ADMIN')
            """)
    int deletePlatformRolesByUserId(@Param("userId") Long userId);

    @Delete("""
            DELETE ur FROM user_role ur
            INNER JOIN `role` r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.code = 'MERCHANT_OWNER'
            """)
    int deleteMerchantOwnerRoleByUserId(@Param("userId") Long userId);
}
