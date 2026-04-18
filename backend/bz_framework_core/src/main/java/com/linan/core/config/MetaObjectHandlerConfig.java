package com.linan.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.linan.core.security.interceptor.AuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 *
 * 负责在插入和更新时自动填充 BaseEntity 中的公共字段：
 * - createTime / updateTime：时间戳
 * - createBy / updateBy：操作者ID（当前登录用户）
 *
 * 工作流程：
 * 1. MyBatis-Plus 在执行 INSERT 或 UPDATE 前调用此处理器
 * 2. 根据 @TableField(fill = ...) 的配置判断哪些字段需要填充
 * 3. 此处理器提供当前时间、当前用户 ID 等值
 *
 * 注意：createBy / updateBy 需要从当前登录用户获取。
 * 当前阶段使用固定值 1（后续登录功能实现后，从 SecurityContext 中获取）。
 */
@Slf4j
@Component
public class MetaObjectHandlerConfig implements MetaObjectHandler {

    /**
     * 插入时的自动填充
     * 当执行 INSERT 操作时，MyBatis-Plus 会调用此方法。
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始自动填充插入字段...");

        // 填充创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // 填充更新时间（插入时也需要设置）
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 填充创建者和更新者ID
        Long currentUserId = getCurrentUserId();
        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    /**
     * 更新时的自动填充
     * 当执行 UPDATE 操作时，MyBatis-Plus 会调用此方法。
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始自动填充更新字段...");

        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 填充更新者ID
        Long currentUserId = getCurrentUserId();
        this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    /**
     * 获取当前登录用户 ID
     * 从 AuthInterceptor 的 ThreadLocal 中获取真实操作用户。
     * 如果未登录（如定时任务触发），则返回 null。
     */
    private Long getCurrentUserId() {
        return AuthInterceptor.getCurrentUserId();
    }
}