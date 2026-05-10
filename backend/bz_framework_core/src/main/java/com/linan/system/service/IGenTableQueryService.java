package com.linan.system.service;

import com.linan.core.gen.ColumnInfo;
import com.linan.core.gen.TableInfo;

import java.util.List;

/**
 * 代码生成 - 数据库表查询服务接口
 *
 * 负责从数据库查询表结构和字段信息
 *
 * @author BareZen Team
 */
public interface IGenTableQueryService {

    /**
     * 查询业务表列表
     *
     * @param tableName 表名（可选，模糊匹配）
     * @return 表信息列表
     */
    List<TableInfo> selectTableList(String tableName);

    /**
     * 查询业务表信息（含字段列表）
     *
     * @param tableName 表名
     * @return 表信息（含字段）
     */
    TableInfo selectTableByName(String tableName);

    /**
     * 查询表的字段信息
     *
     * @param tableName 表名
     * @return 字段信息列表
     */
    List<ColumnInfo> selectColumnsByTable(String tableName);
}