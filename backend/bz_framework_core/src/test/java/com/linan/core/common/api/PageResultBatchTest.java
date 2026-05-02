package com.linan.core.common.api;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PageResult 批量操作测试
 */
class PageResultBatchTest {

    @Test
    void testPageResultWithMultiplePages() {
        List<String> allData = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");

        // Page 1
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<String> page1 = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 3);
        page1.setRecords(allData.subList(0, 3));
        page1.setTotal(10);

        PageResult<String> result1 = PageResult.from(page1);
        assertEquals(1, result1.getPageNum());
        assertEquals(3, result1.getPageSize());
        assertEquals(4, result1.getPages()); // (10+3-1)/3
        assertEquals(3, result1.getList().size());

        // Page 4 (last page)
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<String> page4 = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(4, 3);
        page4.setRecords(allData.subList(9, 10));
        page4.setTotal(10);

        PageResult<String> result4 = PageResult.from(page4);
        assertEquals(4, result4.getPageNum());
        assertEquals(1, result4.getList().size());
    }

    @Test
    void testPageResultEdgeCases() {
        // Zero total
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<String> emptyPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);

        PageResult<String> result = PageResult.from(emptyPage);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getPages());
        assertTrue(result.getList().isEmpty());

        // Single item
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<String> singlePage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        singlePage.setRecords(List.of("only"));
        singlePage.setTotal(1);

        PageResult<String> singleResult = PageResult.from(singlePage);
        assertEquals(1, singleResult.getTotal());
        assertEquals(1, singleResult.getList().size());
    }
}
