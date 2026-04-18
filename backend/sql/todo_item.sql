-- 企业用户端扩展表
-- 待办事项表
CREATE TABLE IF NOT EXISTS `todo_item` (
    `id`             BIGINT       NOT NULL COMMENT '主键ID',
    `title`          VARCHAR(200) NOT NULL COMMENT '待办标题',
    `description`    VARCHAR(500) DEFAULT NULL COMMENT '待办描述',
    `user_id`        BIGINT       NOT NULL COMMENT '所属用户ID',
    `priority`       TINYINT      NOT NULL DEFAULT 1 COMMENT '优先级：0=低 1=中 2=高 3=紧急',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=待办 1=进行中 2=已完成 3=已取消',
    `due_date`       VARCHAR(20)  DEFAULT NULL COMMENT '截止日期',
    `completed_time` VARCHAR(30)  DEFAULT NULL COMMENT '完成时间',
    `create_time`    DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT NULL COMMENT '更新时间',
    `create_by`      BIGINT       DEFAULT NULL COMMENT '创建者',
    `update_by`      BIGINT       DEFAULT NULL COMMENT '更新者',
    `del_flag`       TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除 1=已删除',
    PRIMARY KEY (`id`),
    KEY `idx_todo_user` (`user_id`),
    KEY `idx_todo_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办事项表';
