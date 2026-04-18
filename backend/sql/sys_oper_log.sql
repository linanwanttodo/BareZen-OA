-- ============================================
-- BareZen_Base 操作日志表
-- ============================================

DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除 4授权 5导出 6导入 7强退 8清空数据）',
  `method` varchar(100) DEFAULT '' COMMENT '方法名称（完整类名.方法名）',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `oper_user` varchar(50) DEFAULT '' COMMENT '操作人员',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `class_name` varchar(255) DEFAULT '' COMMENT '类名称',
  `method_name` varchar(100) DEFAULT '' COMMENT '方法名称',
  `oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间（毫秒）',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者ID',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_oper_user` (`oper_user`),
  KEY `idx_oper_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';
