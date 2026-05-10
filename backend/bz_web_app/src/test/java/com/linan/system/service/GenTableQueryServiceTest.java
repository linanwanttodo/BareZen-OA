package com.linan.system.service;

import com.linan.core.gen.ColumnInfo;
import com.linan.core.gen.TableInfo;
import com.linan.web.BareZenApplication;
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
@SpringBootTest(classes = BareZenApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("代码生成表查询Service测试")
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
        assertFalse(tables.isEmpty());
        // barezen_test 由 Flyway V39 建了 asset_category/asset_item/asset_borrow_record，必然命中
        boolean hasAssetTable = tables.stream()
                .anyMatch(t -> t.getTableName().toLowerCase().contains("asset"));
        assertTrue(hasAssetTable, "应返回 asset 相关表");
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
        // 注意：ColumnInfoRowMapper 负责 columnName/columnType/dataType 等元数据，
        // javaType 由 GenTypeMappingService 单独映射（不属于 RowMapper 职责），此处不断言。
        ColumnInfo firstColumn = columns.get(0);
        assertNotNull(firstColumn.getColumnName());
        assertNotNull(firstColumn.getDataType());
        assertNotNull(firstColumn.getColumnType());
        // 主键应被标记（sys_user.id 为 PRI）
        assertTrue(firstColumn.isPk(), "首列应为表主键");
    }
}