package com.linan.core.gen;

import java.util.Set;

/**
 * 代码生成器常量配置
 */
public class GenConstants {

    /** 基表公共字段（不生成到页面表单） */
    public static final Set<String> BASE_FIELDS = Set.of(
            "id", "create_time", "update_time", "create_by", "update_by", "del_flag", "remark");

    /** 文本类型字段 */
    public static final Set<String> TEXT_TYPES = Set.of(
            "varchar", "char", "text", "longtext", "mediumtext");

    /** 数字类型字段 */
    public static final Set<String> NUMBER_TYPES = Set.of(
            "int", "bigint", "tinyint", "smallint", "decimal", "float", "double");

    /** 日期类型字段 */
    public static final Set<String> DATE_TYPES = Set.of(
            "datetime", "timestamp", "date", "time");

    /** 模糊查询字段（按名称自动推断） */
    public static final Set<String> QUERY_LIKE = Set.of(
            "name", "title", "remark", "description", "content", "summary");

    /** 精确查询字段（按名称自动推断） */
    public static final Set<String> QUERY_EQ = Set.of(
            "status", "type", "sex", "flag", "scope", "level",
            "category_id", "user_id", "dept_id", "role_id");

    /** HTML 编辑器字段 */
    public static final Set<String> HTML_EDITOR = Set.of(
            "content", "description", "detail");

    public static final String TEMPLATE_ENTITY = "Entity";
    public static final String TEMPLATE_MAPPER = "Mapper";
    public static final String TEMPLATE_SERVICE = "Service";
    public static final String TEMPLATE_SERVICE_IMPL = "ServiceImpl";
    public static final String TEMPLATE_CONTROLLER = "Controller";
    public static final String TEMPLATE_VUE_INDEX = "VueIndex";

    /** 模板文件基础路径（classpath:templates/gen/） */
    public static final String TPL_ROOT = "templates/gen/";
}
