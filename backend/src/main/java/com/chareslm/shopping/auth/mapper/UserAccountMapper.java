package com.chareslm.shopping.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.auth.entity.UserAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserAccountMapper extends BaseMapper<UserAccount> {
    @Select("""
            SELECT * FROM `user`
            WHERE username = #{identifier} OR email = #{identifier} OR phone = #{identifier}
            LIMIT 1
            """)
    UserAccount selectByLoginIdentifier(@Param("identifier") String identifier);

    @Update("""
            UPDATE `user`
            SET failed_login_count = failed_login_count + 1,
                locked_until = CASE WHEN failed_login_count + 1 >= 5
                    THEN DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 15 MINUTE) ELSE locked_until END
            WHERE id = #{userId}
            """)
    void incrementFailedLoginCount(@Param("userId") Long userId);

    @Update("""
            UPDATE `user`
            SET failed_login_count = 0, locked_until = NULL, last_login_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{userId}
            """)
    void markLoginSucceeded(@Param("userId") Long userId);
}
