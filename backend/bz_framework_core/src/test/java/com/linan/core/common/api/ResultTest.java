package com.linan.core.common.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一返回体 Result 单元测试
 */
class ResultTest {

    @Test
    void testSuccessWithData() {
        Result<String> result = Result.success("hello");

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertEquals("hello", result.getData());
    }

    @Test
    void testSuccessWithCustomMsg() {
        Result<Integer> result = Result.success("自定义成功", 42);

        assertEquals(200, result.getCode());
        assertEquals("自定义成功", result.getMsg());
        assertEquals(42, result.getData());
    }

    @Test
    void testSuccessWithNullData() {
        Result<Void> result = Result.success(null);

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testErrorWithCodeEnum() {
        Result<Void> result = Result.error(ResultCodeEnum.UNAUTHORIZED);

        assertEquals(401, result.getCode());
        assertEquals("未登录或Token已过期", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testErrorWithCustomMsg() {
        Result<Void> result = Result.error("自定义错误");

        assertEquals(500, result.getCode());
        assertEquals("自定义错误", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testForbiddenError() {
        Result<Void> result = Result.error(ResultCodeEnum.FORBIDDEN);

        assertEquals(403, result.getCode());
        assertEquals("没有访问权限", result.getMsg());
    }

    @Test
    void testValidationFailed() {
        Result<Void> result = Result.error(ResultCodeEnum.VALIDATE_FAILED);

        assertEquals(400, result.getCode());
        assertEquals("参数检验失败", result.getMsg());
    }
}