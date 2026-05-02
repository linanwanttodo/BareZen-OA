package com.linan.system.service;

import com.linan.core.gen.ColumnInfo;
import com.linan.core.gen.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * 代码生成服务接口
 */
public interface IGenService {

    /**
     * 查询数据库中所有业务表（排除系统表/quartz表等）
     */
    List<TableInfo> selectTableList(String tableName);

    /**
     * 根据表名查询表及列详细信息
     */
    TableInfo selectTableByName(String tableName);

    /**
     * 生成代码，返回 Map<文件名, 代码内容>
     */
    Map<String, String> generateCode(String tableName);
}
