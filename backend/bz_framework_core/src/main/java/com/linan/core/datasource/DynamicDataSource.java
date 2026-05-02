package com.linan.core.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源（路由数据源）
 *
 * <p>继承 {@link AbstractRoutingDataSource}，实现运行时动态选择数据源的能力。
 * 每次数据库操作时，Spring 会调用 {@link #determineCurrentLookupKey()} 方法获取当前
 * 数据源的查找键，从而决定使用哪个目标数据源。</p>
 *
 * <p>查找键由 {@link DsContextHolder} 提供，可通过 {@link Ds} 注解或直接调用
 * {@link DsContextHolder#set(String)} 来切换数据源。</p>
 *
 * <p>支持配置多个目标数据源（如 master、slave），并在运行时灵活切换。
 * 如果未设置或设置了不存在的键，则使用默认数据源（defaultTargetDataSource）。</p>
 *
 * @see AbstractRoutingDataSource
 * @see DsContextHolder
 * @see Ds
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    /**
     * 确定当前使用的数据源查找键
     *
     * <p>从 {@link DsContextHolder} 中获取当前线程设置的数据源名称，
     * 如果未设置则返回 "master"（默认主数据源）。</p>
     *
     * @return 数据源名称，用于在 targetDataSources 中查找对应的 DataSource
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DsContextHolder.get();
    }
}
