package com.linan.core.gen;

/**
 * 数据库列元信息
 */
public class ColumnInfo {

    /** 列名 */
    private String columnName;

    /** 列注释 */
    private String columnComment;

    /** SQL 列类型（如 varchar(255), bigint） */
    private String columnType;

    /** Java 类型（如 String, Long, LocalDateTime） */
    private String javaType;

    /** Java 字段名（驼峰） */
    private String javaField;

    /** 是否主键 */
    private boolean isPk;

    /** 是否必填 */
    private boolean isRequired;

    /** 是否列表显示 */
    private boolean isList;

    /** 是否查询条件 */
    private boolean isQuery;

    /** 是否编辑 */
    private boolean isEdit;

    /** 是否新增 */
    private boolean isInsert;

    /** 查询类型（EQ / LIKE） */
    private String queryType;

    /** 页面组件类型（input / textarea / editor / select / datetime / number） */
    private String htmlType;

    /** 字典类型 */
    private String dictType;

    // ---- getter / setter ----

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnComment() {
        return columnComment;
    }

    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getJavaField() {
        return javaField;
    }

    public void setJavaField(String javaField) {
        this.javaField = javaField;
    }

    public boolean isPk() {
        return isPk;
    }

    public void setPk(boolean pk) {
        isPk = pk;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean list) {
        isList = list;
    }

    public boolean isQuery() {
        return isQuery;
    }

    public void setQuery(boolean query) {
        isQuery = query;
    }

    public boolean isEdit() {
        return isEdit;
    }

    public void setEdit(boolean edit) {
        isEdit = edit;
    }

    public boolean isInsert() {
        return isInsert;
    }

    public void setInsert(boolean insert) {
        isInsert = insert;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public String getHtmlType() {
        return htmlType;
    }

    public void setHtmlType(String htmlType) {
        this.htmlType = htmlType;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }
}
