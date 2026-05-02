-- ============================================
-- BareZen_Base V14: 修复新模块菜单缺失 + 按钮级权限细化
-- ============================================
-- 背景：V9-V12 新增模块菜单复用了 id 14-21，与 V6 已占用的菜单 id 冲突，
-- INSERT IGNORE 因主键冲突被静默跳过，导致人事/资产/行政等菜单实际未入库。
-- 本迁移用不冲突的新 id（>=10000）重新插入，并补充 F 类型按钮权限。
-- 知识库（id 22/23）无冲突已入库，无需重复插入。

-- 1. 修复缺失的模块目录与菜单（新 id 从 10001 开始）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10001, 0, '人事管理', 'M', NULL, 'hr', NULL, 'user', 5, '0', '1', '人事管理目录'),
(10002, 10001, '员工档案', 'C', 'hr:employee:list', 'hr/employee', 'hr/employee/index', 'peoples', 1, '0', '1', '员工档案菜单'),
(10003, 10001, '请假审批', 'C', 'hr:leave:list', 'hr/leave', 'hr/leave/index', 'calendar', 2, '0', '1', '请假审批菜单'),
(10004, 0, '资产管理', 'M', NULL, 'asset', NULL, 'appstore', 6, '0', '1', '资产管理目录'),
(10005, 10004, '资产台账', 'C', 'asset:item:list', 'asset/item', 'asset/item/index', 'storage', 1, '0', '1', '资产台账菜单'),
(10006, 0, '行政办公', 'M', NULL, 'office', NULL, 'calendar', 7, '0', '1', '行政办公目录'),
(10007, 10006, '会议室预订', 'C', 'office:meeting:list', 'office/meeting', 'office/meeting/index', 'apps', 1, '0', '1', '会议室预订菜单'),
(10008, 10006, '用车申请', 'C', 'office:vehicle:list', 'office/vehicle', 'office/vehicle/index', 'drive', 2, '0', '1', '用车申请菜单');

-- 2. F 类型按钮权限（挂载到对应 C 菜单下）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
-- 员工档案按钮
(10101, 10002, '员工新增', 'F', 'hr:employee:add', NULL, NULL, '#', 1, '0', '1', '员工新增按钮'),
(10102, 10002, '员工修改', 'F', 'hr:employee:edit', NULL, NULL, '#', 2, '0', '1', '员工修改按钮'),
(10103, 10002, '员工删除', 'F', 'hr:employee:delete', NULL, NULL, '#', 3, '0', '1', '员工删除按钮'),
-- 请假审批按钮
(10104, 10003, '请假申请', 'F', 'hr:leave:add', NULL, NULL, '#', 1, '0', '1', '请假申请按钮'),
(10105, 10003, '请假审批', 'F', 'hr:leave:approve', NULL, NULL, '#', 2, '0', '1', '请假审批按钮'),
(10106, 10003, '请假撤销', 'F', 'hr:leave:edit', NULL, NULL, '#', 3, '0', '1', '请假撤销按钮'),
(10107, 10003, '请假删除', 'F', 'hr:leave:delete', NULL, NULL, '#', 4, '0', '1', '请假删除按钮'),
-- 资产台账按钮
(10108, 10005, '资产新增', 'F', 'asset:item:add', NULL, NULL, '#', 1, '0', '1', '资产新增按钮'),
(10109, 10005, '资产修改', 'F', 'asset:item:edit', NULL, NULL, '#', 2, '0', '1', '资产修改按钮'),
(10110, 10005, '资产删除', 'F', 'asset:item:delete', NULL, NULL, '#', 3, '0', '1', '资产删除按钮'),
-- 会议室预订按钮
(10111, 10007, '会议室新增', 'F', 'office:meeting:add', NULL, NULL, '#', 1, '0', '1', '会议室新增按钮'),
(10112, 10007, '会议室修改', 'F', 'office:meeting:edit', NULL, NULL, '#', 2, '0', '1', '会议室修改按钮'),
(10113, 10007, '会议预订删除', 'F', 'office:meeting:delete', NULL, NULL, '#', 3, '0', '1', '会议预订删除按钮'),
-- 用车申请按钮
(10114, 10008, '用车申请', 'F', 'office:vehicle:add', NULL, NULL, '#', 1, '0', '1', '用车申请按钮'),
(10115, 10008, '用车审批', 'F', 'office:vehicle:approve', NULL, NULL, '#', 2, '0', '1', '用车审批按钮'),
(10116, 10008, '用车取消', 'F', 'office:vehicle:edit', NULL, NULL, '#', 3, '0', '1', '用车取消按钮'),
(10117, 10008, '用车删除', 'F', 'office:vehicle:delete', NULL, NULL, '#', 4, '0', '1', '用车删除按钮'),
-- 知识文档按钮（父菜单 id=23 已存在）
(10118, 23, '文档新增', 'F', 'kb:document:add', NULL, NULL, '#', 1, '0', '1', '文档新增按钮'),
(10119, 23, '文档修改', 'F', 'kb:document:edit', NULL, NULL, '#', 2, '0', '1', '文档修改按钮'),
(10120, 23, '文档删除', 'F', 'kb:document:delete', NULL, NULL, '#', 3, '0', '1', '文档删除按钮'),
-- ===== 原有系统模块按钮权限（V3/V6 仅定义了 list 权限，此处补齐）=====
-- 用户管理（父 id=2）
(10201, 2, '用户新增', 'F', 'system:user:add', NULL, NULL, '#', 1, '0', '1', '用户新增按钮'),
(10202, 2, '用户修改', 'F', 'system:user:edit', NULL, NULL, '#', 2, '0', '1', '用户修改按钮'),
(10203, 2, '用户删除', 'F', 'system:user:delete', NULL, NULL, '#', 3, '0', '1', '用户删除按钮'),
-- 角色管理（父 id=3）
(10204, 3, '角色新增', 'F', 'system:role:add', NULL, NULL, '#', 1, '0', '1', '角色新增按钮'),
(10205, 3, '角色修改', 'F', 'system:role:edit', NULL, NULL, '#', 2, '0', '1', '角色修改按钮'),
(10206, 3, '角色删除', 'F', 'system:role:delete', NULL, NULL, '#', 3, '0', '1', '角色删除按钮'),
-- 菜单管理（父 id=4）
(10207, 4, '菜单新增', 'F', 'system:menu:add', NULL, NULL, '#', 1, '0', '1', '菜单新增按钮'),
(10208, 4, '菜单修改', 'F', 'system:menu:edit', NULL, NULL, '#', 2, '0', '1', '菜单修改按钮'),
(10209, 4, '菜单删除', 'F', 'system:menu:delete', NULL, NULL, '#', 3, '0', '1', '菜单删除按钮'),
-- 系统配置（父 id=7）
(10210, 7, '配置修改', 'F', 'system:config:edit', NULL, NULL, '#', 1, '0', '1', '配置修改按钮'),
-- 登录日志（父 id=9）
(10211, 9, '日志删除', 'F', 'monitor:logininfor:delete', NULL, NULL, '#', 1, '0', '1', '日志删除按钮'),
-- 定时任务（父 id=10）
(10212, 10, '任务新增', 'F', 'monitor:job:add', NULL, NULL, '#', 1, '0', '1', '任务新增按钮'),
(10213, 10, '任务修改', 'F', 'monitor:job:edit', NULL, NULL, '#', 2, '0', '1', '任务修改按钮'),
(10214, 10, '任务删除', 'F', 'monitor:job:delete', NULL, NULL, '#', 3, '0', '1', '任务删除按钮'),
-- 代码生成（父 id=12）
(10215, 12, '生成代码', 'F', 'system:gen:code', NULL, NULL, '#', 1, '0', '1', '生成代码按钮'),
-- 数据备份（父 id=13）
(10216, 13, '备份新增', 'F', 'system:backup:add', NULL, NULL, '#', 1, '0', '1', '备份新增按钮'),
(10217, 13, '备份删除', 'F', 'system:backup:delete', NULL, NULL, '#', 2, '0', '1', '备份删除按钮'),
-- 字典管理（父 id=15）
(10218, 15, '字典新增', 'F', 'system:dict:add', NULL, NULL, '#', 1, '0', '1', '字典新增按钮'),
(10219, 15, '字典修改', 'F', 'system:dict:edit', NULL, NULL, '#', 2, '0', '1', '字典修改按钮'),
(10220, 15, '字典删除', 'F', 'system:dict:delete', NULL, NULL, '#', 3, '0', '1', '字典删除按钮'),
-- 部门管理（父 id=16）
(10221, 16, '部门新增', 'F', 'system:dept:add', NULL, NULL, '#', 1, '0', '1', '部门新增按钮'),
(10222, 16, '部门修改', 'F', 'system:dept:edit', NULL, NULL, '#', 2, '0', '1', '部门修改按钮'),
(10223, 16, '部门删除', 'F', 'system:dept:delete', NULL, NULL, '#', 3, '0', '1', '部门删除按钮'),
-- 通知公告（父 id=18）
(10224, 18, '公告新增', 'F', 'system:notice:add', NULL, NULL, '#', 1, '0', '1', '公告新增按钮'),
(10225, 18, '公告修改', 'F', 'system:notice:edit', NULL, NULL, '#', 2, '0', '1', '公告修改按钮'),
(10226, 18, '公告删除', 'F', 'system:notice:delete', NULL, NULL, '#', 3, '0', '1', '公告删除按钮'),
-- 在线用户（父 id=19）
(10227, 19, '用户强退', 'F', 'monitor:online:delete', NULL, NULL, '#', 1, '0', '1', '用户强退按钮'),
-- 操作日志（父 id=20）
(10228, 20, '日志删除', 'F', 'monitor:operlog:delete', NULL, NULL, '#', 1, '0', '1', '日志删除按钮');

-- 3. admin 角色关联修复后的菜单与按钮权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10001), (1, 10002), (1, 10003), (1, 10004), (1, 10005),
(1, 10006), (1, 10007), (1, 10008),
(1, 10101), (1, 10102), (1, 10103),
(1, 10104), (1, 10105), (1, 10106), (1, 10107),
(1, 10108), (1, 10109), (1, 10110),
(1, 10111), (1, 10112), (1, 10113),
(1, 10114), (1, 10115), (1, 10116), (1, 10117),
(1, 10118), (1, 10119), (1, 10120),
(1, 10201), (1, 10202), (1, 10203),
(1, 10204), (1, 10205), (1, 10206),
(1, 10207), (1, 10208), (1, 10209),
(1, 10210), (1, 10211), (1, 10212), (1, 10213), (1, 10214),
(1, 10215), (1, 10216), (1, 10217),
(1, 10218), (1, 10219), (1, 10220),
(1, 10221), (1, 10222), (1, 10223),
(1, 10224), (1, 10225), (1, 10226),
(1, 10227), (1, 10228);
