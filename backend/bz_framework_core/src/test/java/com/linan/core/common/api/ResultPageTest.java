package com.linan.core.common.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result.page() 方法单元测试
 */
class ResultPageTest {

    @Test
    void testPageReturnsWrappedResult() {
        Page<String> page = new Page<>(1, 10);
        page.setRecords(List.of("a", "b"));
        page.setTotal(20);

        Result<PageResult<String>> result = Result.page(page);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(20, result.getData().getTotal());
        assertEquals(2, result.getData().getList().size());
    }

    @Test
    void testPageWithEmptyData() {
        Page<String> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);

        Result<PageResult<String>> result = Result.page(page);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
    }
}
