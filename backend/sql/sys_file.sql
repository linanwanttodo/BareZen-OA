-- ============================================
-- BareZen_Base 文件管理表结构（文件夹+可见范围）
-- ============================================

DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `file_name` varchar(100) DEFAULT '' COMMENT '文件存储名（文件夹时为文件夹名）',
  `original_name` varchar(255) DEFAULT '' COMMENT '原始文件名（文件夹时为空）',
  `file_path` varchar(500) DEFAULT '' COMMENT '文件存储路径（文件夹时为空）',
  `file_url` varchar(500) DEFAULT '' COMMENT '访问URL（文件夹时为空）',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小（字节）',
  `file_type` varchar(50) DEFAULT '' COMMENT '文件类型（image/document/video/audio/other，文件夹时为folder）',
  `storage_type` varchar(20) DEFAULT 'local' COMMENT '存储类型（local/oss）',
  `is_folder` tinyint(1) DEFAULT '0' COMMENT '是否文件夹（0=文件，1=文件夹）',
  `folder_id` bigint DEFAULT '0' COMMENT '父文件夹ID（0=根目录）',
  `scope` varchar(10) DEFAULT 'admin' COMMENT '可见范围（admin=管理端可见，public=所有用户可见）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `del_flag` int DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  PRIMARY KEY (`id`),
  KEY `idx_folder_id` (`folder_id`),
  KEY `idx_scope` (`scope`),
  KEY `idx_is_folder` (`is_folder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件管理记录（支持文件夹结构和可见范围）';
