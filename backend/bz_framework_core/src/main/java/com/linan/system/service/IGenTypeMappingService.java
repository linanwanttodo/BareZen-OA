package com.linan.system.service;

import com.linan.core.gen.ColumnInfo;

/**
 * 代码生成 - 类型映射服务接口
 *
 * 负责数据库类型到Java类型、页面组件等的映射转换
 *
 * @author BareZen Team
 */
public interface IGenTypeMappingService {

    /**
     * 数据库类型映射为Java类型
     *
     * @param dataType 数据库数据类型
     * @return Java类型
     */
    String mapToJavaType(String dataType);

    /**
     * 数据库字段名转换为Java字段名（驼峰命名）
     *
     * @param columnName 数据库字段名
     * @return Java字段名
     */
    String columnToField(String columnName);

    /**
     * 表名转换为Java类名（帕斯卡命名）
     *
     * @param tableName 数据库表名
     * @return Java类名
     */
    String tableToClassName(String tableName);

    /**
     * 解析模块名
     *
     * @param tableName 表名
     * @return 模块名
     */
    String resolveModuleName(String tableName);

    /**
     * 解析权限前缀
     *
     * @param tableName 表名
     * @return 权限前缀
     */
    String resolvePermPrefix(String tableName);

    /**
     * 解析HTML组件类型
     *
     * @param column 字段信息
     * @return HTML组件类型
     */
    String resolveHtmlType(ColumnInfo column);

    /**
     * 解析字典类型
     *
     * @param columnName 字段名
     * @return 字典类型
     */
    String resolveDictType(String columnName);
}