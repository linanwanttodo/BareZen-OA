package com.linan.system.service.impl;

import com.linan.core.gen.ColumnInfo;
import com.linan.core.gen.GenConstants;
import com.linan.core.gen.TableInfo;
import com.linan.system.service.IGenTableQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 代码生成 - 数据库表查询服务实现
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class GenTableQueryServiceImpl implements IGenTableQueryService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<TableInfo> selectTableList(String tableName) {
        StringBuilder sql = new StringBuilder(200);
        sql.append("SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES ")
                .append("WHERE TABLE_SCHEMA = database() ")
                .append("AND TABLE_NAME NOT LIKE 'sys_%' ")
                .append("AND TABLE_NAME NOT LIKE 'art_%' ")
                .append("AND TABLE_NAME NOT LIKE 'QRTZ_%' ");

        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(tableName)) {
            sql.append("AND (TABLE_NAME LIKE ? OR TABLE_COMMENT LIKE ?) ");
            String like = "%" + tableName.trim() + "%";
            params.add(like);
            params.add(like);
        }

        sql.append("ORDER BY TABLE_NAME");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            TableInfo t = new TableInfo();
            t.setTableName(rs.getString("TABLE_NAME"));
            t.setTableComment(rs.getString("TABLE_COMMENT"));
            return t;
        });
    }

    @Override
    public TableInfo selectTableByName(String tableName) {
        // 查询表信息
        String tableSql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = database() AND TABLE_NAME = ?";
        List<TableInfo> tables = jdbcTemplate.query(tableSql, new Object[]{tableName}, (rs, rowNum) -> {
            TableInfo t = new TableInfo();
            t.setTableName(rs.getString("TABLE_NAME"));
            t.setTableComment(rs.getString("TABLE_COMMENT"));
            return t;
        });

        if (tables.isEmpty()) {
            return null;
        }

        TableInfo table = tables.get(0);
        table.setColumns(selectColumnsByTable(tableName));

        return table;
    }

    @Override
    public List<ColumnInfo> selectColumnsByTable(String tableName) {
        String sql = "SELECT COLUMN_NAME, COLUMN_COMMENT, COLUMN_TYPE, DATA_TYPE, " +
                "IFNULL(CHARACTER_MAXIMUM_LENGTH, 0) AS CHAR_LENGTH, " +
                "IFNULL(NUMERIC_PRECISION, 0) AS NUM_PRECISION, " +
                "IFNULL(NUMERIC_SCALE, 0) AS NUM_SCALE, " +
                "COLUMN_KEY, IS_NULLABLE, EXTRA " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = database() AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";

        return jdbcTemplate.query(sql, new Object[]{tableName}, new ColumnInfoRowMapper());
    }

    /**
     * ColumnInfo 行映射器
     */
    private static class ColumnInfoRowMapper implements RowMapper<ColumnInfo> {
        @Override
        public ColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            ColumnInfo col = new ColumnInfo();
            String columnName = rs.getString("COLUMN_NAME");
            col.setColumnName(columnName);
            col.setColumnComment(rs.getString("COLUMN_COMMENT"));
            col.setColumnType(rs.getString("COLUMN_TYPE"));
            col.setDataType(rs.getString("DATA_TYPE"));

            // 主键
            String colKey = rs.getString("COLUMN_KEY");
            col.setPk("PRI".equalsIgnoreCase(colKey));

            // 必填：NOT NULL 且非基表字段
            boolean notNull = "NO".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
            boolean isBase = GenConstants.BASE_FIELDS.contains(columnName);
            col.setRequired(notNull && !isBase && !col.isPk());

            // 页面可见性
            boolean showField = !isBase && !col.isPk();
            col.setList(showField);
            col.setEdit(showField);
            col.setInsert(!col.isPk() && !isBase && !"id".equalsIgnoreCase(columnName));

            // 查询条件
            col.setQuery(GenConstants.QUERY_LIKE.contains(columnName) || GenConstants.QUERY_EQ.contains(columnName));
            if (GenConstants.QUERY_LIKE.contains(columnName)) {
                col.setQueryType("LIKE");
            } else if (GenConstants.QUERY_EQ.contains(columnName)) {
                col.setQueryType("EQ");
            }

            return col;
        }
    }
}