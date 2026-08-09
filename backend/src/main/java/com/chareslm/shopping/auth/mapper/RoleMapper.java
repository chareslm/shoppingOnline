package com.chareslm.shopping.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.auth.entity.Role;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {
    @Select("SELECT * FROM `role` WHERE code = #{code} AND status = 'ACTIVE'")
    Role selectActiveByCode(@Param("code") String code);

    @Select("""
            SELECT r.code FROM `role` r
            INNER JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 'ACTIVE'
            """)
    List<String> selectCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM `role` WHERE status = 'ACTIVE' ORDER BY code, id")
    List<Role> selectAllActive();

    @Select("""
            SELECT COUNT(*) FROM user_role ur
            INNER JOIN `role` r ON r.id = ur.role_id
            WHERE r.code = #{roleCode}
            """)
    long countUsersByRoleCode(@Param("roleCode") String roleCode);
}
