package com.linan.core.gen;

import java.util.List;

/**
 * 数据库表元信息
 */
public class TableInfo {

    /** 表名称 */
    private String tableName;

    /** 表注释 */
    private String tableComment;

    /** 类名（首字母大写驼峰） */
    private String className;

    /** 类变量名（首字母小写驼峰） */
    private String classVar;

    /** 模块名（如 system / article） */
    private String moduleName;

    /** 业务权限前缀（如 system:notice） */
    private String permPrefix;

    /** 列信息列表 */
    private List<ColumnInfo> columns;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableComment() {
        return tableComment;
    }

    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassVar() {
        return classVar;
    }

    public void setClassVar(String classVar) {
        this.classVar = classVar;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getPermPrefix() {
        return permPrefix;
    }

    public void setPermPrefix(String permPrefix) {
        this.permPrefix = permPrefix;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
        this.columns = columns;
    }
}
