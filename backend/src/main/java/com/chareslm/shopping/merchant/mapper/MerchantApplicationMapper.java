package com.chareslm.shopping.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.merchant.entity.MerchantApplication;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商家申请持久化入口，审核写操作通过“主键 + 期望旧状态”实现状态机 CAS。
 */
public interface MerchantApplicationMapper extends BaseMapper<MerchantApplication> {
    @Select("""
            <script>
            SELECT * FROM merchant_application
            <where><if test="status != null and status != ''">status = #{status}</if></where>
            ORDER BY id DESC LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<MerchantApplication> selectPage(@Param("status") String status, @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    @Select("""
            <script>
            SELECT COUNT(*) FROM merchant_application
            <where><if test="status != null and status != ''">status = #{status}</if></where>
            </script>
            """)
    long countPage(@Param("status") String status);

    @Update("""
            UPDATE merchant_application SET status = #{nextStatus}, rejection_reason = #{reason},
                qualification_audited_by = #{auditorId}, qualification_audited_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND status = 'SUBMITTED'
            """)
    // 返回 0 表示申请不再处于待资质审核状态，调用方必须报告并发/重复操作冲突。
    int auditQualification(@Param("id") Long id, @Param("nextStatus") String nextStatus,
                           @Param("reason") String reason, @Param("auditorId") Long auditorId);

    @Update("""
            UPDATE merchant_application SET status = 'ACCOUNT_APPROVED', account_user_id = #{userId},
                account_reused = #{reused}, account_audited_by = #{auditorId},
                account_audited_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND status = 'QUALIFICATION_APPROVED'
            """)
    // 账号及店铺在同一事务中创建；CAS 失败会触发整个事务回滚。
    int approveAccount(@Param("id") Long id, @Param("userId") Long userId,
                       @Param("reused") boolean reused, @Param("auditorId") Long auditorId);

    @Update("""
            UPDATE merchant_application SET status = 'REJECTED', rejection_reason = #{reason},
                account_audited_by = #{auditorId}, account_audited_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND status = 'QUALIFICATION_APPROVED'
            """)
    int rejectAccount(@Param("id") Long id, @Param("reason") String reason, @Param("auditorId") Long auditorId);

    @Update("UPDATE merchant_application SET email_delivery_status = #{status} WHERE id = #{id}")
    int updateEmailStatus(@Param("id") Long id, @Param("status") String status);
}
