-- ============================================
-- BareZen_Base V30: 开放平台（Open API）能力
-- ============================================
-- 背景：对应大型企业 OA「系统集成」需求（市场调研 78.6% 企业将集成能力列为前三考量）。
-- 提供两类能力：
--   1) sys_api_key：外部系统通过 API Key 调用开放接口（/api/open/**），
--      密钥仅存 SHA-256 哈希（同密码策略，明文只在生成时返回一次）。
--   2) sys_webhook：事件订阅回调（审批完成/公告发布等），事件发生时向配置的
--      URL 推送 JSON 回调，实现「OA 事件 → 外部系统」的实时联动。

-- 1. 开放平台 API Key
CREATE TABLE IF NOT EXISTS `sys_api_key` (
  `id`          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
  `key_name`    VARCHAR(50)  NOT NULL COMMENT '密钥名称（用途标识）',
  `api_key_hash` VARCHAR(64) NOT NULL COMMENT 'API Key SHA-256 哈希（不存明文）',
  `scopes`      VARCHAR(255) DEFAULT NULL COMMENT '权限范围，逗号分隔，如 open:approval:read',
  `status`      CHAR(1)      DEFAULT '0' COMMENT '状态：0=启用 1=禁用',
  `expire_time` DATETIME     DEFAULT NULL COMMENT '过期时间，NULL=永不过期',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT       DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`    TINYINT      DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_key_hash` (`api_key_hash`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台 API Key';

-- 2. Webhook 事件订阅
CREATE TABLE IF NOT EXISTS `sys_webhook` (
  `id`          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
  `name`        VARCHAR(50)  NOT NULL COMMENT '订阅名称',
  `url`         VARCHAR(500) NOT NULL COMMENT '回调地址（HTTP/HTTPS）',
  `secret`      VARCHAR(64)  DEFAULT NULL COMMENT '签名密钥（可选，用于 HMAC-SHA256 签名校验）',
  `events`      VARCHAR(255) NOT NULL COMMENT '订阅事件，逗号分隔：approval.completed / notice.published',
  `status`      CHAR(1)      DEFAULT '0' COMMENT '状态：0=启用 1=禁用',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT       DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`    TINYINT      DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook 事件订阅';
