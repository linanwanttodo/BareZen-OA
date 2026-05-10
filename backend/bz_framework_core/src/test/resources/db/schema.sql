-- 测试环境数据库表结构
-- 用于H2内存数据库的测试初始化

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL UNIQUE,
    nick_name VARCHAR(50),
    password VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    sex CHAR(1),
    avatar VARCHAR(255),
    status VARCHAR(1) DEFAULT '0',
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME,
    update_time DATETIME,
    del_flag INT DEFAULT 0
);

-- 资产分类表
CREATE TABLE IF NOT EXISTS asset_category (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME,
    update_time DATETIME,
    del_flag INT DEFAULT 0
);

-- 资产项目表
CREATE TABLE IF NOT EXISTS asset_item (
    id BIGINT PRIMARY KEY,
    asset_name VARCHAR(100) NOT NULL,
    asset_code VARCHAR(50),
    category_id BIGINT,
    status VARCHAR(1) DEFAULT '0',
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME,
    update_time DATETIME,
    del_flag INT DEFAULT 0
);

-- 资产借用记录表
CREATE TABLE IF NOT EXISTS asset_borrow_record (
    id BIGINT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    borrower_id BIGINT NOT NULL,
    borrower_name VARCHAR(50) NOT NULL,
    borrow_time VARCHAR(20),
    expect_return_time VARCHAR(20),
    return_time VARCHAR(20),
    remark VARCHAR(500),
    status VARCHAR(1) DEFAULT '0',
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME,
    update_time DATETIME,
    del_flag INT DEFAULT 0
);

-- 请假申请表
CREATE TABLE IF NOT EXISTS att_leave_request (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(50),
    start_time VARCHAR(20),
    end_time VARCHAR(20),
    leave_type VARCHAR(10),
    leave_days INT,
    reason VARCHAR(500),
    status VARCHAR(1) DEFAULT '0',
    approver_id BIGINT,
    approver_name VARCHAR(50),
    approve_time VARCHAR(20),
    approve_remark VARCHAR(500),
    assignee_id BIGINT,
    assignee_name VARCHAR(50),
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME,
    update_time DATETIME,
    del_flag INT DEFAULT 0
);

-- 添加唯一索引
CREATE UNIQUE INDEX uk_asset_category_name ON asset_category(name);
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user(user_name);