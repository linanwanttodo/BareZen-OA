package com.linan.core.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分页返回体 PageResult 单元测试
 */
class PageResultTest {

    @Test
    void testFromIPage() {
        // 准备
        Page<String> page = new Page<>(1, 10);
        page.setRecords(List.of("a", "b", "c"));
        page.setTotal(100);

        // 执行
        PageResult<String> result = PageResult.from(page);

        // 验证
        assertEquals(100, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(10, result.getPages()); // (100+10-1)/10 = 10
        assertEquals(3, result.getList().size());
        assertEquals("a", result.getList().get(0));
    }

    @Test
    void testFromEmptyPage() {
        Page<String> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);

        PageResult<String> result = PageResult.from(page);

        assertEquals(0, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(0, result.getPages());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testPageResultGettersAndSetters() {
        PageResult<String> result = new PageResult<>();
        result.setTotal(50L);
        result.setPageNum(2L);
        result.setPageSize(20L);
        result.setPages(3L);
        result.setList(List.of("x"));

        assertEquals(50, result.getTotal());
        assertEquals(2, result.getPageNum());
        assertEquals(20, result.getPageSize());
        assertEquals(3, result.getPages());
        assertEquals(1, result.getList().size());
    }
}