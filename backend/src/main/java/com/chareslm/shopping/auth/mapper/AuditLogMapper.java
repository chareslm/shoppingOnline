package com.chareslm.shopping.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.auth.entity.AuditLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogMapper extends BaseMapper<AuditLog> {
    @Select("""
            <script>
            SELECT a.*, u.username AS actor_username
            FROM audit_log a
            LEFT JOIN `user` u ON u.id = a.actor_user_id
            <where>
              <if test="actorKeyword != null">
                AND (u.username LIKE CONCAT('%', #{actorKeyword}, '%')
                  OR CAST(a.actor_user_id AS CHAR) = #{actorKeyword})
              </if>
              <if test="module != null">AND a.module = #{module}</if>
              <if test="actionCode != null">AND a.action_code = #{actionCode}</if>
              <if test="success != null">AND a.success = #{success}</if>
              <if test="startAt != null">AND a.created_at &gt;= #{startAt}</if>
              <if test="endAt != null">AND a.created_at &lt;= #{endAt}</if>
            </where>
            ORDER BY a.created_at DESC, a.id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<AuditLog> selectAdminPage(@Param("actorKeyword") String actorKeyword,
                                   @Param("module") String module,
                                   @Param("actionCode") String actionCode,
                                   @Param("success") Boolean success,
                                   @Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM audit_log a
            LEFT JOIN `user` u ON u.id = a.actor_user_id
            <where>
              <if test="actorKeyword != null">
                AND (u.username LIKE CONCAT('%', #{actorKeyword}, '%')
                  OR CAST(a.actor_user_id AS CHAR) = #{actorKeyword})
              </if>
              <if test="module != null">AND a.module = #{module}</if>
              <if test="actionCode != null">AND a.action_code = #{actionCode}</if>
              <if test="success != null">AND a.success = #{success}</if>
              <if test="startAt != null">AND a.created_at &gt;= #{startAt}</if>
              <if test="endAt != null">AND a.created_at &lt;= #{endAt}</if>
            </where>
            </script>
            """)
    long countAdminPage(@Param("actorKeyword") String actorKeyword,
                        @Param("module") String module,
                        @Param("actionCode") String actionCode,
                        @Param("success") Boolean success,
                        @Param("startAt") LocalDateTime startAt,
                        @Param("endAt") LocalDateTime endAt);
}
