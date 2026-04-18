-- =============================================
-- BareZen_Base 收藏 & 阅读历史 建表 SQL
-- =============================================

CREATE TABLE IF NOT EXISTS `art_favorite` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `article_id`    BIGINT        NOT NULL COMMENT '文章ID',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章收藏表';

CREATE TABLE IF NOT EXISTS `art_read_history` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`           BIGINT        NOT NULL COMMENT '用户ID',
    `article_id`        BIGINT        NOT NULL COMMENT '文章ID',
    `article_title`     VARCHAR(200)  DEFAULT NULL COMMENT '文章标题（冗余，加速列表查询）',
    `article_summary`   VARCHAR(500)  DEFAULT NULL COMMENT '文章摘要（冗余）',
    `author_name`       VARCHAR(50)   DEFAULT NULL COMMENT '作者名（冗余）',
    `create_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阅读历史表';
