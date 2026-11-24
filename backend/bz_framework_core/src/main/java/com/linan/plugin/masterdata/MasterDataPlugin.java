package com.linan.plugin.masterdata;

import com.linan.core.plugin.AbstractPlugin;
import com.linan.core.plugin.PluginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 主数据底座插件
 *
 * <p>第一个按插件门控机制交付的业务插件：接口位于 com.linan.plugin.masterdata
 * 包下，由 PluginGateAspect 按本插件（pluginId = com.linan.plugin.masterdata）
 * 的门控状态拦截；清单 resources/plugins/masterdata-plugin.json 声明
 * controller/service/entity/权限/前端路由，启动期由 PluginManifestLoader
 * 读取并校验声明完整性。</p>
 *
 * 功能：
 * - 供应商信息管理
 * - 客户信息管理
 * - 物料信息管理
 * - 单据号规则发号（供采购/销售等后续模块复用）
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class MasterDataPlugin extends AbstractPlugin {

    @Override
    public String getPluginId() {
        return "com.linan.plugin.masterdata";
    }

    @Override
    public String getPluginName() {
        return "主数据模块";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "提供供应商、客户、物料主数据管理与单据号发号服务";
    }

    @Override
    public String getAuthor() {
        return "BareZen Team";
    }

    @Override
    public void initialize() throws PluginException {
        log.info("初始化主数据插件...");

        // 1. 检查 V58 迁移产物（主数据表/单据号规则表）已就位
        // 2. 校验清单声明的权限点与菜单已注册

        super.initialize();
        log.info("主数据插件初始化完成");
    }

    @Override
    public void start() throws PluginException {
        log.info("启动主数据插件...");

        // 1. 预热单据号规则缓存（如后续引入缓存）
        // 2. 对外提供 /api/masterdata/v1/** 接口（门控随本插件状态放行）

        super.start();
        log.info("主数据插件启动完成");
    }

    @Override
    public void stop() throws PluginException {
        log.info("停止主数据插件...");

        // 1. 清理缓存
        // 2. 门控关闭后 /api/masterdata/v1/** 拒绝服务（503）

        super.stop();
        log.info("主数据插件停止完成");
    }
}
