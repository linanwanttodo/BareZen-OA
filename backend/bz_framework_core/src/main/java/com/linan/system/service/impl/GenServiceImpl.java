package com.linan.system.service.impl;

import com.linan.core.gen.ColumnInfo;
import com.linan.core.gen.GenConstants;
import com.linan.core.gen.TableInfo;
import com.linan.system.service.IGenService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成服务实现
 */
@Service
public class GenServiceImpl implements IGenService {

    private final JdbcTemplate jdbcTemplate;

    public GenServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ======================== 查询 ========================

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

        // 填充衍生字段
        String className = tableToClassName(tableName);
        table.setClassName(className);
        table.setClassVar(Character.toLowerCase(className.charAt(0)) + className.substring(1));
        table.setModuleName(resolveModuleName(tableName));
        table.setPermPrefix(table.getModuleName() + ":" + toPermName(tableName));

        return table;
    }

    private List<ColumnInfo> selectColumnsByTable(String tableName) {
        String sql = "SELECT COLUMN_NAME, COLUMN_COMMENT, COLUMN_TYPE, DATA_TYPE, " +
                "IFNULL(CHARACTER_MAXIMUM_LENGTH, 0) AS CHAR_LENGTH, " +
                "IFNULL(NUMERIC_PRECISION, 0) AS NUM_PRECISION, " +
                "IFNULL(NUMERIC_SCALE, 0) AS NUM_SCALE, " +
                "COLUMN_KEY, IS_NULLABLE, EXTRA " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = database() AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";

        return jdbcTemplate.query(sql, new Object[]{tableName}, (rs, rowNum) -> {
            ColumnInfo col = new ColumnInfo();
            String columnName = rs.getString("COLUMN_NAME");
            col.setColumnName(columnName);
            col.setColumnComment(rs.getString("COLUMN_COMMENT"));
            col.setColumnType(rs.getString("COLUMN_TYPE"));
            col.setJavaType(mapToJavaType(rs.getString("DATA_TYPE")));
            col.setJavaField(columnToField(columnName));

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

            // 页面组件
            col.setHtmlType(resolveHtmlType(col));

            // 字典类型（按名称约定推断）
            if (columnName.endsWith("_type") || columnName.endsWith("_status")) {
                col.setDictType(resolveDictType(columnName));
            }

            return col;
        });
    }

    // ======================== 类型映射 ========================

    private String mapToJavaType(String dataType) {
        if (dataType == null) return "String";
        return switch (dataType.toLowerCase()) {
            case "bigint" -> "Long";
            case "int", "tinyint", "smallint", "mediumint" -> "Integer";
            case "decimal", "numeric" -> "BigDecimal";
            case "float", "double" -> "Double";
            case "varchar", "char", "text", "longtext", "mediumtext", "tinytext", "json" -> "String";
            case "date" -> "LocalDate";
            case "time" -> "LocalTime";
            case "datetime", "timestamp" -> "LocalDateTime";
            case "blob", "longblob", "mediumblob" -> "byte[]";
            default -> "String";
        };
    }

    private String resolveHtmlType(ColumnInfo col) {
        String colName = col.getColumnName();
        String javaType = col.getJavaType();
        String rawType = col.getColumnType().toLowerCase();

        // 富文本编辑器
        if (GenConstants.HTML_EDITOR.contains(colName)) {
            return "editor";
        }
        // 日期时间
        if (rawType.contains("datetime") || rawType.contains("timestamp")) {
            return "datetime";
        }
        if (rawType.contains("date")) {
            return "date";
        }
        // 大文本
        if (rawType.contains("text") || rawType.contains("mediumtext") || rawType.contains("longtext")) {
            return "textarea";
        }
        // 数字
        if ("Integer".equals(javaType) || "Long".equals(javaType) || "BigDecimal".equals(javaType) || "Double".equals(javaType)) {
            // 带字典的用 select
            if (col.getDictType() != null) {
                return "select";
            }
            return "number";
        }
        // 字典类型用 select
        if (col.getDictType() != null) {
            return "select";
        }
        return "input";
    }

    private String resolveDictType(String columnName) {
        // 按命名约定推断字典类型
        if (columnName.endsWith("_type")) {
            String prefix = columnName.substring(0, columnName.length() - 5);
            return prefix + "_type";
        }
        if (columnName.endsWith("_status")) {
            String prefix = columnName.substring(0, columnName.length() - 7);
            return prefix + "_status";
        }
        return null;
    }

    // ======================== 命名转换 ========================

    private String tableToClassName(String tableName) {
        // 移除前缀（如 sys_notice -> notice, art_article -> article）
        String name = tableName;
        if (name.contains("_")) {
            int idx = name.indexOf('_');
            name = name.substring(idx + 1);
        }
        // 下划线转驼峰首字母大写
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }

    private String columnToField(String columnName) {
        // 下划线转驼峰首字母小写
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : columnName.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }

    private String resolveModuleName(String tableName) {
        if (tableName == null) return "system";
        String lower = tableName.toLowerCase();
        if (lower.startsWith("sys_")) return "system";
        return "system";
    }

    private String toPermName(String tableName) {
        // sys_notice -> notice, todo_item -> todo_item
        String name = tableName;
        if (name.contains("_")) {
            int idx = name.indexOf('_');
            name = name.substring(idx + 1);
        }
        return name.replace('_', ':');
    }

    @Override
    public Map<String, String> generateCode(String tableName) {
        TableInfo table = selectTableByName(tableName);
        if (table == null) {
            throw new RuntimeException("表 " + tableName + " 不存在");
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put(table.getClassName() + ".java", getEntityCode(table));
        result.put(table.getClassName() + "Mapper.java", getMapperCode(table));
        result.put("I" + table.getClassName() + "Service.java", getServiceCode(table));
        result.put(table.getClassName() + "ServiceImpl.java", getServiceImplCode(table));
        result.put(table.getClassName() + "Controller.java", getControllerCode(table));
        result.put("index.vue", getVueCode(table));
        return result;
    }

    // ======================== 模板方法 ========================

    private String getEntityCode(TableInfo table) {
        StringBuilder sb = new StringBuilder(512);
        String pkg = "com.linan." + table.getModuleName() + ".entity";
        sb.append("package ").append(pkg).append(";\n\n");

        // imports
        sb.append("import com.baomidou.mybatisplus.annotation.TableName;\n");
        sb.append("import com.linan.core.common.entity.BaseEntity;\n");
        sb.append("import lombok.Data;\n");
        sb.append("import lombok.EqualsAndHashCode;\n");

        Set<String> extraImports = new LinkedHashSet<>();
        for (ColumnInfo col : table.getColumns()) {
            if ("BigDecimal".equals(col.getJavaType())) extraImports.add("import java.math.BigDecimal;");
            if ("LocalDateTime".equals(col.getJavaType())) extraImports.add("import java.time.LocalDateTime;");
            if ("LocalDate".equals(col.getJavaType())) extraImports.add("import java.time.LocalDate;");
            if ("LocalTime".equals(col.getJavaType())) extraImports.add("import java.time.LocalTime;");
            if ("LocalDateTime".equals(col.getJavaType())) extraImports.add("import com.fasterxml.jackson.annotation.JsonFormat;");
        }
        for (String imp : extraImports) {
            if (imp.startsWith("import com.fasterxml.jackson")) {
                sb.append(imp).append('\n');
            }
        }
        for (String imp : extraImports) {
            if (!imp.startsWith("import com.fasterxml.jackson")) {
                sb.append(imp).append('\n');
            }
        }
        sb.append('\n');

        String comment = StringUtils.hasText(table.getTableComment()) ? table.getTableComment() : table.getTableName();
        sb.append("/** ").append(comment).append("实体，对应 ").append(table.getTableName()).append(" */\n");
        sb.append("@Data\n");
        sb.append("@EqualsAndHashCode(callSuper = true)\n");
        sb.append("@TableName(\"").append(table.getTableName()).append("\")\n");
        sb.append("public class ").append(table.getClassName()).append(" extends BaseEntity {\n\n");

        for (ColumnInfo col : table.getColumns()) {
            // 跳过基类中已有的字段
            if (GenConstants.BASE_FIELDS.contains(col.getColumnName())) continue;

            if (StringUtils.hasText(col.getColumnComment())) {
                sb.append("    /** ").append(col.getColumnComment()).append(" */\n");
            }
            // 如果是 LocalDateTime 类型，加上 @JsonFormat
            if ("LocalDateTime".equals(col.getJavaType())) {
                sb.append("    @JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")\n");
            }
            if ("LocalDate".equals(col.getJavaType())) {
                sb.append("    @JsonFormat(pattern = \"yyyy-MM-dd\")\n");
            }
            sb.append("    private ").append(col.getJavaType()).append(" ").append(col.getJavaField()).append(";\n\n");
        }

        sb.append('}');
        return sb.toString();
    }

    private String getMapperCode(TableInfo table) {
        StringBuilder sb = new StringBuilder(256);
        String pkg = "com.linan." + table.getModuleName() + ".mapper";
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".entity.").append(table.getClassName()).append(";\n");
        sb.append("import org.apache.ibatis.annotations.Mapper;\n\n");

        String comment = StringUtils.hasText(table.getTableComment()) ? table.getTableComment() : table.getTableName();
        sb.append("/** ").append(comment).append(" Mapper */\n");
        sb.append("@Mapper\n");
        sb.append("public interface ").append(table.getClassName()).append("Mapper extends BaseMapper<").append(table.getClassName()).append("> {\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String getServiceCode(TableInfo table) {
        StringBuilder sb = new StringBuilder(256);
        String pkg = "com.linan." + table.getModuleName() + ".service";
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.baomidou.mybatisplus.extension.service.IService;\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".entity.").append(table.getClassName()).append(";\n\n");

        String comment = StringUtils.hasText(table.getTableComment()) ? table.getTableComment() : table.getTableName();
        sb.append("/** ").append(comment).append(" 服务接口 */\n");
        sb.append("public interface I").append(table.getClassName()).append("Service extends IService<").append(table.getClassName()).append("> {\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String getServiceImplCode(TableInfo table) {
        StringBuilder sb = new StringBuilder(256);
        String pkg = "com.linan." + table.getModuleName() + ".service.impl";
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".entity.").append(table.getClassName()).append(";\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".mapper.").append(table.getClassName()).append("Mapper;\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".service.I").append(table.getClassName()).append("Service;\n");
        sb.append("import org.springframework.stereotype.Service;\n\n");

        String comment = StringUtils.hasText(table.getTableComment()) ? table.getTableComment() : table.getTableName();
        sb.append("/** ").append(comment).append(" 服务实现 */\n");
        sb.append("@Service\n");
        sb.append("public class ").append(table.getClassName()).append("ServiceImpl")
                .append(" extends ServiceImpl<").append(table.getClassName()).append("Mapper, ").append(table.getClassName()).append(">")
                .append(" implements I").append(table.getClassName()).append("Service {\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String getControllerCode(TableInfo table) {
        StringBuilder sb = new StringBuilder(512);
        String pkg = "com.linan." + table.getModuleName() + ".controller";
        sb.append("package ").append(pkg).append(";\n\n");

        sb.append("import com.linan.core.common.annotation.OperLog;\n");
        sb.append("import com.linan.core.common.api.PageResult;\n");
        sb.append("import com.linan.core.common.api.Result;\n");
        sb.append("import com.linan.core.common.controller.BaseController;\n");
        sb.append("import com.linan.core.security.RequirePermission;\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".entity.").append(table.getClassName()).append(";\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".service.I").append(table.getClassName()).append("Service;\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        sb.append("import org.springframework.web.bind.annotation.*;\n\n");

        String comment = StringUtils.hasText(table.getTableComment()) ? table.getTableComment() : table.getTableName();
        String perm = table.getPermPrefix();
        String mapping = table.getClassVar();

        sb.append("/** ").append(comment).append(" 控制器 */\n");
        sb.append("@RestController\n");
        sb.append("@RequestMapping(\"/api/").append(table.getModuleName()).append("/v1/").append(mapping).append("s\")\n");
        sb.append("public class ").append(table.getClassName()).append("Controller")
                .append(" extends BaseController<").append(table.getClassName()).append(", I").append(table.getClassName()).append("Service> {\n\n");

        sb.append("    @Autowired\n");
        sb.append("    private I").append(table.getClassName()).append("Service ").append(table.getClassVar()).append("Service;\n\n");

        sb.append("    @Override\n");
        sb.append("    protected I").append(table.getClassName()).append("Service getService() { return ").append(table.getClassVar()).append("Service; }\n\n");

        // list
        sb.append("    @RequirePermission(\"").append(perm).append(":list\")\n");
        sb.append("    @GetMapping(\"/page\")\n");
        sb.append("    @Override\n");
        sb.append("    public Result<PageResult<").append(table.getClassName()).append(">> list(\n");
        sb.append("            @RequestParam(defaultValue = \"1\") Integer pageNum,\n");
        sb.append("            @RequestParam(defaultValue = \"10\") Integer pageSize) {\n");
        sb.append("        return super.list(pageNum, pageSize);\n");
        sb.append("    }\n\n");

        // getById
        sb.append("    @RequirePermission(\"").append(perm).append(":list\")\n");
        sb.append("    @GetMapping(\"/{id}\")\n");
        sb.append("    @Override\n");
        sb.append("    public Result<").append(table.getClassName()).append("> getById(@PathVariable Long id) {\n");
        sb.append("        return super.getById(id);\n");
        sb.append("    }\n\n");

        // save
        sb.append("    @OperLog(title = \"").append(comment).append("\", businessType = 1)\n");
        sb.append("    @RequirePermission(\"").append(perm).append(":add\")\n");
        sb.append("    @PostMapping\n");
        sb.append("    @Override\n");
        sb.append("    public Result<Void> save(@jakarta.validation.Valid @RequestBody ").append(table.getClassName()).append(" entity) {\n");
        sb.append("        return super.save(entity);\n");
        sb.append("    }\n\n");

        // update
        sb.append("    @OperLog(title = \"").append(comment).append("\", businessType = 2)\n");
        sb.append("    @RequirePermission(\"").append(perm).append(":edit\")\n");
        sb.append("    @PutMapping\n");
        sb.append("    @Override\n");
        sb.append("    public Result<Void> update(@jakarta.validation.Valid @RequestBody ").append(table.getClassName()).append(" entity) {\n");
        sb.append("        return super.update(entity);\n");
        sb.append("    }\n\n");

        // delete
        sb.append("    @OperLog(title = \"").append(comment).append("\", businessType = 3)\n");
        sb.append("    @RequirePermission(\"").append(perm).append(":delete\")\n");
        sb.append("    @DeleteMapping(\"/{id}\")\n");
        sb.append("    @Override\n");
        sb.append("    public Result<Void> delete(@PathVariable Long id) {\n");
        sb.append("        return super.delete(id);\n");
        sb.append("    }\n\n");

        sb.append('}');
        return sb.toString();
    }

    private String getVueCode(TableInfo table) {
        String comment = StringUtils.hasText(table.getTableComment()) ? table.getTableComment() : table.getTableName();
        String perm = table.getPermPrefix();
        String className = table.getClassName();
        String classVar = table.getClassVar();

        // 收集需要显示的列（非 base 字段）
        List<ColumnInfo> listCols = table.getColumns().stream()
                .filter(c -> c.isList() && !GenConstants.BASE_FIELDS.contains(c.getColumnName()))
                .collect(Collectors.toList());
        // 收集表单字段
        List<ColumnInfo> formCols = table.getColumns().stream()
                .filter(c -> (c.isEdit() || c.isInsert()) && !GenConstants.BASE_FIELDS.contains(c.getColumnName()))
                .collect(Collectors.toList());
        // 收集查询字段
        List<ColumnInfo> queryCols = table.getColumns().stream()
                .filter(ColumnInfo::isQuery)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder(2048);
        sb.append("<template>\n");
        sb.append("  <div class=\"page-container\">\n");

        // ---- 搜索区域 ----
        if (!queryCols.isEmpty()) {
            sb.append("    <!-- 搜索区域 -->\n");
            sb.append("    <a-card class=\"mb-4\" :bordered=\"true\">\n");
            sb.append("      <a-form :model=\"searchForm\" layout=\"inline\" @submit-success=\"handleSearch\">\n");
            for (ColumnInfo col : queryCols) {
                String field = col.getJavaField();
                String label = StringUtils.hasText(col.getColumnComment()) ? col.getColumnComment() : field;
                if ("select".equals(col.getHtmlType())) {
                    String dict = col.getDictType() != null ? col.getDictType() : field;
                    sb.append("        <a-form-item field=\"").append(field).append("\" label=\"").append(label).append("\">\n");
                    sb.append("          <a-select v-model=\"searchForm.").append(field).append("\" placeholder=\"请选择\" allow-clear style=\"width: 150px\">\n");
                    sb.append("            <a-option value=\"\">全部</a-option>\n");
                    sb.append("          </a-select>\n");
                    sb.append("        </a-form-item>\n");
                } else {
                    sb.append("        <a-form-item field=\"").append(field).append("\" label=\"").append(label).append("\">\n");
                    sb.append("          <a-input v-model=\"searchForm.").append(field)
                            .append("\" placeholder=\"请输入").append(label).append("\" allow-clear style=\"width: 180px\" />\n");
                    sb.append("        </a-form-item>\n");
                }
            }
            sb.append("        <a-form-item>\n");
            sb.append("          <a-space>\n");
            sb.append("            <a-button type=\"primary\" html-type=\"submit\">搜索</a-button>\n");
            sb.append("            <a-button @click=\"handleReset\">重置</a-button>\n");
            sb.append("          </a-space>\n");
            sb.append("        </a-form-item>\n");
            sb.append("      </a-form>\n");
            sb.append("    </a-card>\n\n");
        }

        // ---- 表格区域 ----
        sb.append("    <!-- 表格区域 -->\n");
        sb.append("    <a-card :bordered=\"true\">\n");
        sb.append("      <template #title>\n");
        sb.append("        <div class=\"flex items-center justify-between w-full\">\n");
        sb.append("          <span>").append(comment).append("</span>\n");
        sb.append("          <a-button type=\"primary\" @click=\"handleAdd\">\n");
        sb.append("            <template #icon><icon-plus /></template>\n");
        sb.append("            新增\n");
        sb.append("          </a-button>\n");
        sb.append("        </div>\n");
        sb.append("      </template>\n\n");
        sb.append("      <a-spin :loading=\"loading\">\n");
        sb.append("        <a-table\n");
        sb.append("          :data=\"tableData\"\n");
        sb.append("          :pagination=\"false\"\n");
        sb.append("          row-key=\"id\"\n");
        sb.append("        >\n");
        sb.append("          <template #columns>\n");
        // id 列
        sb.append("            <a-table-column title=\"ID\" data-index=\"id\" :width=\"80\" />\n");
        for (ColumnInfo col : listCols) {
            String field = col.getJavaField();
            String label = StringUtils.hasText(col.getColumnComment()) ? col.getColumnComment() : field;
            if ("datetime".equals(col.getHtmlType())) {
                sb.append("            <a-table-column title=\"").append(label).append("\" :width=\"160\">\n");
                sb.append("              <template #cell=\"{ record }\">\n");
                sb.append("                {{ formatTime(record.").append(field).append(") }}\n");
                sb.append("              </template>\n");
                sb.append("            </a-table-column>\n");
            } else {
                sb.append("            <a-table-column title=\"").append(label).append("\" data-index=\"").append(field).append("\" />\n");
            }
        }
        // 操作列
        sb.append("            <a-table-column title=\"操作\" :width=\"200\" align=\"right\">\n");
        sb.append("              <template #cell=\"{ record }\">\n");
        sb.append("                <a-space>\n");
        sb.append("                  <a-button size=\"small\" @click=\"handleEdit(record)\">编辑</a-button>\n");
        sb.append("                  <a-popconfirm content=\"确认删除？\" @ok=\"handleDelete(record)\">\n");
        sb.append("                    <a-button size=\"small\" status=\"danger\">删除</a-button>\n");
        sb.append("                  </a-popconfirm>\n");
        sb.append("                </a-space>\n");
        sb.append("              </template>\n");
        sb.append("            </a-table-column>\n");
        sb.append("          </template>\n");
        sb.append("        </a-table>\n\n");
        sb.append("        <div class=\"mt-4 flex justify-end\">\n");
        sb.append("          <a-pagination\n");
        sb.append("            v-model:current=\"pagination.current\"\n");
        sb.append("            v-model:page-size=\"pagination.pageSize\"\n");
        sb.append("            :total=\"pagination.total\"\n");
        sb.append("            show-total\n");
        sb.append("            show-jumper\n");
        sb.append("            @change=\"onPageChange\"\n");
        sb.append("          />\n");
        sb.append("        </div>\n");
        sb.append("      </a-spin>\n");
        sb.append("    </a-card>\n\n");

        // ---- 新增/编辑弹窗 ----
        sb.append("    <!-- 新增/编辑弹窗 -->\n");
        sb.append("    <a-modal\n");
        sb.append("      v-model:visible=\"formDialogVisible\"\n");
        sb.append("      :title=\"formType === 'add' ? '新增").append(comment).append("' : '编辑").append(comment).append("'\"\n");
        sb.append("      :width=\"640\"\n");
        sb.append("      :mask-closable=\"false\"\n");
        sb.append("      @ok=\"onFormConfirm\"\n");
        sb.append("      @cancel=\"formDialogVisible = false\"\n");
        sb.append("    >\n");
        sb.append("      <a-form :model=\"formData\" layout=\"vertical\">\n");

        for (ColumnInfo col : formCols) {
            String field = col.getJavaField();
            String label = StringUtils.hasText(col.getColumnComment()) ? col.getColumnComment() : field;
            String requiredAttr = col.isRequired() ? " :rules=\"{ required: true, message: '请填写" + label + "' }\"" : "";

            sb.append("        <a-form-item field=\"").append(field).append("\" label=\"").append(label).append("\"");
            if (col.isRequired()) {
                sb.append(" :rules=\"{ required: true, message: '请填写").append(label).append("' }\"");
            }
            sb.append(">\n");

            switch (col.getHtmlType()) {
                case "textarea":
                    sb.append("          <a-textarea v-model=\"formData.").append(field)
                            .append("\" placeholder=\"请输入").append(label).append("\" />\n");
                    break;
                case "editor":
                    sb.append("          <a-textarea v-model=\"formData.").append(field)
                            .append("\" placeholder=\"请输入").append(label).append("\" :rows=\"6\" />\n");
                    break;
                case "select":
                    String dict = col.getDictType() != null ? col.getDictType() : field;
                    sb.append("          <a-select v-model=\"formData.").append(field)
                            .append("\" placeholder=\"请选择").append(label).append("\" />\n");
                    break;
                case "datetime":
                    sb.append("          <a-date-picker v-model=\"formData.").append(field)
                            .append("\" show-time style=\"width: 100%\" />\n");
                    break;
                case "date":
                    sb.append("          <a-date-picker v-model=\"formData.").append(field)
                            .append("\" style=\"width: 100%\" />\n");
                    break;
                case "number":
                    sb.append("          <a-input-number v-model=\"formData.").append(field)
                            .append("\" placeholder=\"请输入").append(label).append("\" style=\"width: 100%\" />\n");
                    break;
                default:
                    sb.append("          <a-input v-model=\"formData.").append(field)
                            .append("\" placeholder=\"请输入").append(label).append("\" />\n");
                    break;
            }
            sb.append("        </a-form-item>\n");
        }

        sb.append("      </a-form>\n");
        sb.append("    </a-modal>\n");
        sb.append("  </div>\n");
        sb.append("</template>\n\n");

        // ---- Script ----
        sb.append("<script setup lang=\"ts\">\n");
        sb.append("import { ref, reactive, onMounted } from 'vue'\n");
        sb.append("import { Message } from '@arco-design/web-vue'\n");
        sb.append("import { IconPlus } from '@arco-design/web-vue/es/icon'\n");
        sb.append("import {\n");
        sb.append("  getPageApi,\n");
        sb.append("  getByIdApi,\n");
        sb.append("  addApi,\n");
        sb.append("  updateApi,\n");
        sb.append("  deleteApi,\n");
        sb.append("  type ").append(className).append(",\n");
        sb.append("  type ").append(className).append("ListParams,\n");
        sb.append("} from '@/api/").append(table.getModuleName()).append("/").append(classVar).append("'\n\n");

        // search
        if (!queryCols.isEmpty()) {
            sb.append("// ============ 搜索 ============\n");
            sb.append("const searchForm = reactive<").append(className).append("ListParams>({\n");
            for (int i = 0; i < queryCols.size(); i++) {
                ColumnInfo col = queryCols.get(i);
                String field = col.getJavaField();
                String defaultVal = "''";
                sb.append("  ").append(field).append(": ").append(defaultVal);
                if (i < queryCols.size() - 1) sb.append(',');
                sb.append('\n');
            }
            sb.append("})\n\n");

            sb.append("function handleSearch() {\n");
            sb.append("  pagination.current = 1\n");
            sb.append("  loadTable()\n");
            sb.append("}\n\n");

            sb.append("function handleReset() {\n");
            for (ColumnInfo col : queryCols) {
                sb.append("  searchForm.").append(col.getJavaField()).append(" = ''\n");
            }
            sb.append("  pagination.current = 1\n");
            sb.append("  loadTable()\n");
            sb.append("}\n\n");
        }

        // table
        sb.append("// ============ 表格 ============\n");
        sb.append("const loading = ref(false)\n");
        sb.append("const tableData = ref<").append(className).append("[]>([])\n\n");
        sb.append("const pagination = reactive({ current: 1, pageSize: 10, total: 0 })\n\n");
        sb.append("function onPageChange(page: number) {\n");
        sb.append("  pagination.current = page\n");
        sb.append("  loadTable()\n");
        sb.append("}\n\n");

        sb.append("async function loadTable() {\n");
        sb.append("  loading.value = true\n");
        sb.append("  try {\n");
        sb.append("    const res = await getPageApi({\n");
        sb.append("      pageNum: pagination.current,\n");
        sb.append("      pageSize: pagination.pageSize,\n");
        for (ColumnInfo col : queryCols) {
            sb.append("      ").append(col.getJavaField()).append(": searchForm.").append(col.getJavaField()).append(" || undefined,\n");
        }
        sb.append("    })\n");
        sb.append("    tableData.value = res.data?.list || []\n");
        sb.append("    pagination.total = res.data?.total || 0\n");
        sb.append("  } catch {\n");
        sb.append("    /* interceptor handles */\n");
        sb.append("  } finally {\n");
        sb.append("    loading.value = false\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        // formatTime
        sb.append("function formatTime(time: string) {\n");
        sb.append("  if (!time) return '-'\n");
        sb.append("  return time.replace('T', ' ').substring(0, 16)\n");
        sb.append("}\n\n");

        // delete
        sb.append("// ============ 删除 ============\n");
        sb.append("async function handleDelete(row: ").append(className).append(") {\n");
        sb.append("  try {\n");
        sb.append("    await deleteApi(row.id)\n");
        sb.append("    Message.success('删除成功')\n");
        sb.append("    loadTable()\n");
        sb.append("  } catch {\n");
        sb.append("    /* interceptor handles */\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        // form
        sb.append("// ============ 新增/编辑 ============\n");
        sb.append("const formDialogVisible = ref(false)\n");
        sb.append("const formType = ref<'add' | 'edit'>('add')\n\n");

        sb.append("const defaultFormData: Partial<").append(className).append("> = {\n");
        for (int i = 0; i < formCols.size(); i++) {
            ColumnInfo col = formCols.get(i);
            String field = col.getJavaField();
            String defVal;
            if ("Integer".equals(col.getJavaType()) || "Long".equals(col.getJavaType())) {
                defVal = "undefined";
            } else if ("BigDecimal".equals(col.getJavaType()) || "Double".equals(col.getJavaType())) {
                defVal = "undefined";
            } else if ("LocalDateTime".equals(col.getJavaType()) || "LocalDate".equals(col.getJavaType())) {
                defVal = "undefined";
            } else {
                defVal = "''";
            }
            sb.append("  ").append(field).append(": ").append(defVal);
            if (i < formCols.size() - 1) sb.append(',');
            sb.append('\n');
        }
        sb.append("}\n\n");

        sb.append("const formData = reactive<Partial<").append(className).append(">>({ ...defaultFormData })\n\n");

        sb.append("function handleAdd() {\n");
        sb.append("  formType.value = 'add'\n");
        sb.append("  Object.assign(formData, { ...defaultFormData })\n");
        sb.append("  formDialogVisible.value = true\n");
        sb.append("}\n\n");

        sb.append("function handleEdit(row: ").append(className).append(") {\n");
        sb.append("  formType.value = 'edit'\n");
        sb.append("  Object.assign(formData, {\n");
        sb.append("    id: row.id,\n");
        for (ColumnInfo col : formCols) {
            sb.append("    ").append(col.getJavaField()).append(": row.").append(col.getJavaField()).append(",\n");
        }
        sb.append("  })\n");
        sb.append("  formDialogVisible.value = true\n");
        sb.append("}\n\n");

        sb.append("async function onFormConfirm() {\n");
        sb.append("  try {\n");
        sb.append("    if (formType.value === 'add') {\n");
        sb.append("      await addApi({ ...formData })\n");
        sb.append("      Message.success('新增成功')\n");
        sb.append("    } else {\n");
        sb.append("      await updateApi({ ...formData })\n");
        sb.append("      Message.success('修改成功')\n");
        sb.append("    }\n");
        sb.append("    formDialogVisible.value = false\n");
        sb.append("    loadTable()\n");
        sb.append("  } catch {\n");
        sb.append("    /* interceptor handles */\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        sb.append("// ============ 初始化 ============\n");
        sb.append("onMounted(() => {\n");
        sb.append("  loadTable()\n");
        sb.append("})\n");
        sb.append("</script>\n\n");
        sb.append("<style scoped>\n");
        sb.append("</style>\n");

        return sb.toString();
    }
}
