package com.chareslm.shopping.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.auth.entity.Permission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PermissionMapper extends BaseMapper<Permission> {
    @Select("""
            SELECT DISTINCT p.code FROM permission p
            INNER JOIN role_permission rp ON rp.permission_id = p.id
            INNER JOIN user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId} AND p.status = 'ACTIVE'
            """)
    List<String> selectCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM permission WHERE status = 'ACTIVE' ORDER BY resource, action, id")
    List<Permission> selectAllActive();
}
