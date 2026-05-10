package com.linan.plugin.salary;

import com.linan.core.plugin.AbstractPlugin;
import com.linan.core.plugin.PluginException;
import com.linan.core.plugin.PluginStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工资管理插件
 *
 * 示例插件，展示如何实现一个完整的业务插件
 *
 * 功能：
 * - 工资结构管理
 * - 工资计算
 * - 工资发放记录
 * - 工资报表统计
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class SalaryPlugin extends AbstractPlugin {

    @Override
    public String getPluginId() {
        return "com.linan.plugin.salary";
    }

    @Override
    public String getPluginName() {
        return "工资管理模块";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "提供员工工资结构管理、工资计算、发放记录和报表统计功能";
    }

    @Override
    public String getAuthor() {
        return "BareZen Team";
    }

    @Override
    public void initialize() throws PluginException {
        log.info("初始化工资管理插件...");
        
        // 1. 检查数据库表是否存在
        // 2. 初始化配置数据
        // 3. 注册权限点
        // 4. 注册菜单项
        
        super.initialize();
        log.info("工资管理插件初始化完成");
    }

    @Override
    public void start() throws PluginException {
        log.info("启动工资管理插件...");
        
        // 1. 启动定时任务（如每月工资计算）
        // 2. 注册Controller
        // 3. 注册Service
        // 4. 加载前端路由配置
        
        super.start();
        log.info("工资管理插件启动完成");
    }

    @Override
    public void stop() throws PluginException {
        log.info("停止工资管理插件...");
        
        // 1. 停止定时任务
        // 2. 注销Controller
        // 3. 清理缓存
        // 4. 释放资源
        
        super.stop();
        log.info("工资管理插件停止完成");
    }
}