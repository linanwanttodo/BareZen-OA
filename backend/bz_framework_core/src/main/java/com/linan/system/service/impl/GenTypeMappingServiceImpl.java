package com.linan.system.service.impl;

import com.linan.core.gen.ColumnInfo;
import com.linan.core.gen.GenConstants;
import com.linan.system.service.IGenTypeMappingService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成 - 类型映射服务实现
 *
 * @author BareZen Team
 */
@Service
public class GenTypeMappingServiceImpl implements IGenTypeMappingService {

    /**
     * 数据库类型到Java类型的映射
     */
    private static final Map<String, String> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("char", "String");
        TYPE_MAP.put("varchar", "String");
        TYPE_MAP.put("text", "String");
        TYPE_MAP.put("tinytext", "String");
        TYPE_MAP.put("mediumtext", "String");
        TYPE_MAP.put("longtext", "String");
        TYPE_MAP.put("tinyint", "Integer");
        TYPE_MAP.put("smallint", "Integer");
        TYPE_MAP.put("int", "Integer");
        TYPE_MAP.put("integer", "Integer");
        TYPE_MAP.put("bigint", "Long");
        TYPE_MAP.put("float", "Float");
        TYPE_MAP.put("double", "Double");
        TYPE_MAP.put("decimal", "java.math.BigDecimal");
        TYPE_MAP.put("numeric", "java.math.BigDecimal");
        TYPE_MAP.put("date", "java.time.LocalDate");
        TYPE_MAP.put("datetime", "java.time.LocalDateTime");
        TYPE_MAP.put("timestamp", "java.time.LocalDateTime");
        TYPE_MAP.put("time", "java.time.LocalTime");
        TYPE_MAP.put("bit", "Boolean");
        TYPE_MAP.put("boolean", "Boolean");
    }

    @Override
    public String mapToJavaType(String dataType) {
        if (dataType == null) return "String";
        String lower = dataType.toLowerCase();
        // 处理带长度的类型，如 varchar(255)
        if (lower.contains("(")) {
            lower = lower.substring(0, lower.indexOf("("));
        }
        return TYPE_MAP.getOrDefault(lower, "String");
    }

    @Override
    public String columnToField(String columnName) {
        if (columnName == null) return "";
        StringBuilder result = new StringBuilder();
        boolean upperCase = false;
        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            if (c == '_') {
                upperCase = true;
            } else {
                if (upperCase) {
                    result.append(Character.toUpperCase(c));
                    upperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }

    @Override
    public String tableToClassName(String tableName) {
        if (tableName == null) return "";
        // 移除表前缀（如 sys_, biz_ 等）
        String name = tableName.replaceAll("^(sys_|biz_|hr_|office_|asset_|kb_)", "");
        // 转换为帕斯卡命名
        StringBuilder result = new StringBuilder();
        boolean upperCase = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                upperCase = true;
            } else {
                if (upperCase) {
                    result.append(Character.toUpperCase(c));
                    upperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }

    @Override
    public String resolveModuleName(String tableName) {
        if (tableName == null) return "system";
        if (tableName.startsWith("sys_")) return "system";
        if (tableName.startsWith("hr_")) return "hr";
        if (tableName.startsWith("office_")) return "office";
        if (tableName.startsWith("asset_")) return "asset";
        if (tableName.startsWith("kb_")) return "kb";
        return "system";
    }

    @Override
    public String resolvePermPrefix(String tableName) {
        String moduleName = resolveModuleName(tableName);
        String permName = toPermName(tableName);
        return moduleName + ":" + permName;
    }

    @Override
    public String resolveHtmlType(ColumnInfo column) {
        String javaType = column.getJavaType();
        String columnName = column.getColumnName();

        // 根据字段名推断
        if (columnName.contains("password") || columnName.contains("pwd")) {
            return "password";
        }
        if (columnName.contains("email")) {
            return "email";
        }
        if (columnName.contains("mobile") || columnName.contains("phone")) {
            return "mobile";
        }
        if (columnName.contains("url") || columnName.contains("link")) {
            return "url";
        }
        if (columnName.contains("date") || columnName.contains("time")) {
            return "datetime";
        }
        if (columnName.contains("image") || columnName.contains("img") || columnName.contains("avatar")) {
            return "imageUpload";
        }
        if (columnName.contains("file") || columnName.contains("document")) {
            return "fileUpload";
        }
        if (columnName.contains("editor") || columnName.contains("content") || columnName.contains("remark")) {
            return "editor";
        }

        // 根据Java类型推断
        if ("Integer".equals(javaType) || "Long".equals(javaType)) {
            return "input";
        }
        if ("java.math.BigDecimal".equals(javaType) || "Double".equals(javaType) || "Float".equals(javaType)) {
            return "input";
        }
        if ("java.time.LocalDate".equals(javaType) || "java.time.LocalDateTime".equals(javaType)) {
            return "datetime";
        }
        if ("Boolean".equals(javaType)) {
            return "radio";
        }

        return "input";
    }

    @Override
    public String resolveDictType(String columnName) {
        if (columnName == null) return "";
        String lower = columnName.toLowerCase();
        if (lower.endsWith("_type") || lower.endsWith("_status")) {
            return lower.replace("_type", "").replace("_status", "");
        }
        return "";
    }

    /**
     * 表名转换为权限名称（小写+下划线）
     */
    private String toPermName(String tableName) {
        if (tableName == null) return "";
        // 移除表前缀
        String name = tableName.replaceAll("^(sys_|biz_|hr_|office_|asset_|kb_)", "");
        return name.toLowerCase();
    }
}