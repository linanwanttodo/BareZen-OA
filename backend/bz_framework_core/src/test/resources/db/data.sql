-- 测试环境初始数据
-- 用于H2内存数据库的测试数据初始化

-- 插入测试用户
INSERT INTO sys_user (id, user_name, nick_name, password, email, phone, sex, status, create_time, update_time, del_flag) VALUES
(1, 'admin', '管理员', '$2a$10$testPasswordHash', 'admin@example.com', '13800138000', '0', '0', NOW(), NOW(), 0),
(2, 'testuser', '测试用户', '$2a$10$testPasswordHash', 'test@example.com', '13800138001', '0', '0', NOW(), NOW(), 0);

-- 插入测试资产分类
INSERT INTO asset_category (id, name, parent_id, sort, status, create_time, update_time, del_flag) VALUES
(1, '办公设备', 0, 1, 1, NOW(), NOW(), 0),
(2, '电脑设备', 1, 1, 1, NOW(), NOW(), 0),
(3, '办公家具', 0, 2, 1, NOW(), NOW(), 0);

-- 插入测试资产项目
INSERT INTO asset_item (id, asset_name, asset_code, category_id, status, create_time, update_time, del_flag) VALUES
(1, '笔记本电脑', 'LAPTOP-001', 2, '0', NOW(), NOW(), 0),
(2, '办公桌', 'DESK-001', 3, '0', NOW(), NOW(), 0),
(3, '显示器', 'MONITOR-001', 2, '0', NOW(), NOW(), 0);