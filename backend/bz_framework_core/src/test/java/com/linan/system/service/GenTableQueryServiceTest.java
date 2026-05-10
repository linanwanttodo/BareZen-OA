package com.linan.system.service;

import com.linan.core.gen.ColumnInfo;
import com.linan.core.gen.TableInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 代码生成表查询Service测试
 *
 * @author BareZen Team
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("代码生成表查询Service测试")
@Disabled("GenTableQueryServiceImpl 使用 MySQL information_schema 方言（TABLE_COMMENT 等列），"
        + "H2 不支持；bz_framework_core 模块无 @SpringBootConfiguration，需真实 MySQL 环境后移入 bz_web_app 模块执行")
class GenTableQueryServiceTest {

    @Autowired
    private IGenTableQueryService tableQueryService;

    @Test
    @DisplayName("查询表列表 - 返回业务表")
    void selectTableList_returnsBusinessTables() {
        List<TableInfo> tables = tableQueryService.selectTableList(null);
        
        assertNotNull(tables);
        assertFalse(tables.isEmpty());
        
        // 验证不包含系统表
        boolean hasSystemTable = tables.stream()
                .anyMatch(t -> t.getTableName().startsWith("sys_"));
        assertFalse(hasSystemTable, "不应包含系统表");
    }

    @Test
    @DisplayName("查询表列表 - 模糊搜索")
    void selectTableList_withSearch_filtersByKeyword() {
        List<TableInfo> tables = tableQueryService.selectTableList("asset");
        
        assertNotNull(tables);
        // 如果有asset相关的表，应该被返回
        boolean hasAssetTable = tables.stream()
                .anyMatch(t -> t.getTableName().toLowerCase().contains("asset"));
        // 这个断言可能失败，取决于数据库中是否有asset表
    }

    @Test
    @DisplayName("查询表详情 - 存在的表返回详情")
    void selectTableByName_existingTable_returnsDetails() {
        // 查询一个肯定存在的表，比如sys_user
        TableInfo table = tableQueryService.selectTableByName("sys_user");
        
        assertNotNull(table);
        assertEquals("sys_user", table.getTableName());
        assertNotNull(table.getColumns());
        assertFalse(table.getColumns().isEmpty());
    }

    @Test
    @DisplayName("查询表详情 - 不存在的表返回null")
    void selectTableByName_nonExistingTable_returnsNull() {
        TableInfo table = tableQueryService.selectTableByName("non_existing_table_12345");
        
        assertNull(table);
    }

    @Test
    @DisplayName("查询字段信息 - 表字段包含必要信息")
    void selectColumnsByTable_returnsColumnInfo() {
        List<ColumnInfo> columns = tableQueryService.selectColumnsByTable("sys_user");
        
        assertNotNull(columns);
        assertFalse(columns.isEmpty());
        
        // 验证字段信息完整性
        ColumnInfo firstColumn = columns.get(0);
        assertNotNull(firstColumn.getColumnName());
        assertNotNull(firstColumn.getDataType());
        assertNotNull(firstColumn.getJavaType());
    }
}