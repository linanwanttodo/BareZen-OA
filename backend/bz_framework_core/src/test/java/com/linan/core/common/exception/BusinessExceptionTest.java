package com.linan.core.common.exception;

import com.linan.core.common.api.ResultCodeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务异常 BusinessException 单元测试
 */
class BusinessExceptionTest {

    @Test
    void testThrowWithMessage() {
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            throw new BusinessException("自定义错误消息");
        });

        assertEquals(500, ex.getCode());
        assertEquals("自定义错误消息", ex.getMessage());
    }

    @Test
    void testThrowWithResultCodeEnum() {
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        });

        assertEquals(401, ex.getCode());
        assertEquals("未登录或Token已过期", ex.getMessage());
    }

    @Test
    void testThrowWithResultCodeEnumAndCustomMessage() {
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "自定义无权限消息");
        });

        assertEquals(403, ex.getCode());
        assertEquals("自定义无权限消息", ex.getMessage());
    }

    @Test
    void testThrowInServiceFlow() {
        // 模拟 Service 层抛出业务异常
        try {
            simulateService("test");
            fail("Should throw BusinessException");
        } catch (BusinessException e) {
            assertEquals(500, e.getCode());
            assertEquals("用户名已存在", e.getMessage());
        }
    }

    private void simulateService(String username) {
        if ("test".equals(username)) {
            throw new BusinessException("用户名已存在");
        }
    }
}