package com.linan.core.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 统一分页返回体
 *
 * 用于包装 MyBatis-Plus 的 IPage 分页结果，
 * 返回给前端一个标准化的分页 JSON 结构。
 *
 * 相比直接返回 IPage，PageResult 有以下好处：
 * 1. 字段名与前端约定完全一致（total/pageNum/pageSize/pages/list）
 * 2. 避免暴露 MyBatis-Plus 内部类给前端
 * 3. 可以灵活扩展字段（如 summary 汇总信息）
 *
 * @param <T> 列表中的数据类型
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long pageNum;

    /** 每页显示条数 */
    private long pageSize;

    /** 总页数：(total + pageSize - 1) / pageSize */
    private long pages;

    /** 当前页的数据列表 */
    private List<T> list;

    /**
     * 从 MyBatis-Plus 的 IPage 对象构建 PageResult
     *
     * @param page MyBatis-Plus 分页查询结果
     * @param <T>  数据类型
     * @return PageResult 实例
     */
    public static <T> PageResult<T> from(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());          // 总记录数
        result.setPageNum(page.getCurrent());      // 当前页码（IPage.getCurrent()）
        result.setPageSize(page.getSize());        // 每页条数（IPage.getSize()）
        result.setPages(page.getPages());          // 总页数（IPage.getPages()）
        result.setList(page.getRecords());         // 当前页数据（IPage.getRecords()）
        return result;
    }
}