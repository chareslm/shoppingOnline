package com.chareslm.shopping.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.auth.entity.UserAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserAccountMapper extends BaseMapper<UserAccount> {
    @Select("""
            SELECT * FROM `user`
            WHERE username = #{identifier} OR email = #{identifier} OR phone = #{identifier}
            LIMIT 1
            """)
    UserAccount selectByLoginIdentifier(@Param("identifier") String identifier);

    @Select("""
            <script>
            SELECT * FROM `user`
            <where>
              <if test="keyword != null">
                AND (username LIKE CONCAT('%', #{keyword}, '%')
                  OR email LIKE CONCAT('%', #{keyword}, '%')
                  OR phone LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="status != null">AND status = #{status}</if>
            </where>
            ORDER BY id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<UserAccount> selectAdminPage(@Param("keyword") String keyword, @Param("status") String status,
                                      @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("""
            <script>
            SELECT COUNT(*) FROM `user`
            <where>
              <if test="keyword != null">
                AND (username LIKE CONCAT('%', #{keyword}, '%')
                  OR email LIKE CONCAT('%', #{keyword}, '%')
                  OR phone LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="status != null">AND status = #{status}</if>
            </where>
            </script>
            """)
    long countAdminPage(@Param("keyword") String keyword, @Param("status") String status);

    @Update("UPDATE `user` SET password_hash = #{passwordHash} WHERE id = #{userId}")
    int updatePasswordHash(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

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
