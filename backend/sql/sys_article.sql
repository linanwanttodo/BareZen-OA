-- =============================================
-- BareZen_Base 内容管理模块 SQL
-- article / category / comment 表
-- =============================================

CREATE TABLE IF NOT EXISTS `art_category` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`          VARCHAR(50)   NOT NULL COMMENT '分类名称',
    `description`   VARCHAR(255)   DEFAULT NULL COMMENT '分类描述',
    `order_num`     INT           DEFAULT 0 COMMENT '显示顺序',
    `status`        TINYINT       DEFAULT 1 COMMENT '状态 (0=停用, 1=正常)',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章分类表';

CREATE TABLE IF NOT EXISTS `art_article` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `author_id`     BIGINT        NOT NULL COMMENT '作者用户ID',
    `author_name`   VARCHAR(50)   DEFAULT NULL COMMENT '作者昵称',
    `category_id`   BIGINT        DEFAULT NULL COMMENT '分类ID',
    `category_name` VARCHAR(50)   DEFAULT NULL COMMENT '分类名称',
    `title`         VARCHAR(200)   NOT NULL COMMENT '文章标题',
    `summary`       VARCHAR(500)  DEFAULT NULL COMMENT '文章摘要',
    `content`       LONGTEXT      NOT NULL COMMENT '文章正文 (HTML)',
    `cover_image`   VARCHAR(500)  DEFAULT NULL COMMENT '封面图URL',
    `view_count`    INT           DEFAULT 0 COMMENT '浏览量',
    `like_count`    INT           DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT           DEFAULT 0 COMMENT '评论数',
    `status`        TINYINT       DEFAULT 1 COMMENT '状态 (0=草稿, 1=已发布)',
    `tags`          VARCHAR(255)  DEFAULT NULL COMMENT '标签 (逗号分隔)',
    `order_num`     INT           DEFAULT 0 COMMENT '排序',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

CREATE TABLE IF NOT EXISTS `art_comment` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `article_id`   BIGINT        NOT NULL COMMENT '文章ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `user_name`     VARCHAR(50)   DEFAULT NULL COMMENT '用户名',
    `user_avatar`   VARCHAR(500)  DEFAULT NULL COMMENT '用户头像',
    `content`       VARCHAR(1000) NOT NULL COMMENT '评论内容',
    `parent_id`     BIGINT        DEFAULT NULL COMMENT '父评论ID (回复)',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- =============================================
-- 初始化数据
-- =============================================

-- 分类
INSERT INTO `art_category` (`id`, `name`, `description`, `order_num`, `status`) VALUES
(1, '技术', '技术文章、框架解析、踩坑记录', 1, 1),
(2, '产品', '产品设计、需求分析、用户体验', 2, 1),
(3, '设计', 'UI设计、UX研究、设计系统', 3, 1),
(4, '运营', '运营策略、数据分析、增长黑客', 4, 1);

-- 示例文章
INSERT INTO `art_article` (`id`, `author_id`, `author_name`, `category_id`, `category_name`, `title`, `summary`, `content`, `status`, `tags`, `view_count`, `like_count`, `comment_count`, `order_num`) VALUES
(1, 1, '超级管理员', 1, '技术',
 'BareZen_Base 极简模块化全栈框架详解',
 '深度解析 BareZen_Base 的模块化架构设计理念，探讨如何通过微内核+插件模式实现真正的按需加载。',
 '<h2>一、背景</h2><p>当前 Java 开发生态中，以若依为代表的后台框架虽然功能强大，但对于追求极致效率的小型团队来说，存在过度耦合的问题...</p><h2>二、架构设计</h2><p>BareZen_Base 采用微内核 + 插件架构...</p><h2>三、核心特性</h2><ul><li>极致轻量，按需组装</li><li>纯净 JWT，无 Spring Security 包袱</li><li>Java 21 虚拟线程压榨 I/O 性能</li></ul>',
 1, 'Java,SpringBoot,架构', 1256, 89, 12, 1),

(2, 1, '超级管理员', 1, '技术',
 'Vue 3 Composition API 最佳实践',
 '深入探索 Vue 3 Composition API 的核心概念，从 setup() 到 script setup，从 ref 到 reactive，从 useXxx Hooks 到 Pinia 状态管理。',
 '<h2>一、setup() 函数</h2><p>setup() 是 Composition API 的入口...</p><h2>二、响应式系统</h2><p>Vue 3 的响应式系统基于 Proxy 实现...</p>',
 1, 'Vue3,TypeScript,前端', 892, 67, 8, 2),

(3, 1, '超级管理员', 2, '产品',
 '从零设计一个 B2B SaaS 产品',
 '分享我在设计 BareZen_Base 产品过程中的思考：从市场调研、用户访谈、需求建模到 MVP 验证的完整流程。',
 '<h2>一、市场调研</h2><p>在开始之前，我们首先需要明确...</p><h2>二、用户访谈</h2><p>通过 1v1 访谈了解目标用户的真实痛点...</p>',
 1, '产品设计,SaaS', 654, 45, 5, 3),

(4, 1, '超级管理员', 3, '设计',
 'Tailwind CSS vs 传统 CSS：如何选择',
 '全面对比 Tailwind CSS 和传统 CSS 的优劣势，结合实际项目经验给出选型建议。',
 '<h2>一、Tailwind CSS 是什么</h2><p>Utility-First CSS...</p>',
 1, 'CSS,Tailwind,设计', 423, 34, 3, 4),

(5, 1, '超级管理员', 1, '技术',
 'MyBatis-Plus 3.5 完全指南',
 'MyBatis-Plus 是 MyBatis 的增强工具，本指南覆盖 CRUD、Lambda 查询、分页插件、自动填充等核心功能。',
 '<h2>一、快速开始</h2><p>引入依赖后...</p><h2>二、Lambda 查询</h2><p>告别字符串列名...</p>',
 1, 'MyBatis,Java,数据库', 1103, 78, 15, 5),

(6, 1, '超级管理员', 4, '运营',
 '如何通过数据分析驱动产品迭代',
 '介绍如何使用漏斗分析、留存分析、A/B 测试等数据方法，做出更科学的产品决策。',
 '<h2>一、数据驱动的重要性</h2><p>数据不会说谎...</p>',
 1, '数据分析,运营,增长', 312, 23, 2, 6);

-- 示例评论
INSERT INTO `art_comment` (`id`, `article_id`, `user_id`, `user_name`, `content`) VALUES
(1, 1, 1, '超级管理员', '写得很好，终于有一个真正轻量的框架了！'),
(2, 1, 1, '超级管理员', '期待后续的插件生态发展'),
(3, 2, 1, '超级管理员', 'Composition API 确实比 Options API 更灵活'),
(4, 5, 1, '超级管理员', '分页插件配置那块写得非常清楚');
