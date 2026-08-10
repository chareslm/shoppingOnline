package com.chareslm.shopping.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.chareslm.shopping.security.context.CurrentUser;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 审计字段自动填充。
 * <p>
 * 与 {@code BaseEntity} 的 FieldFill 配合：插入时填充 createdAt/updatedAt/createdBy/updatedBy，
 * 更新时填充 updatedAt/updatedBy。createdBy/updatedBy 取自认证上下文；
 * 定时任务等无认证上下文场景填充 null。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        Long userId = currentUserId();
        this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
        this.strictInsertFill(metaObject, "updatedBy", Long.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserId());
    }

    private Long currentUserId() {
        try {
            return CurrentUser.require().userId();
        } catch (Exception e) {
            // 定时任务、系统内部操作等无认证上下文场景
            return null;
        }
    }
}