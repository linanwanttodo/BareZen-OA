package com.linan.web.controller;

import com.linan.core.common.api.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CommonController 公共端点（健康检查）单元测试
 */
class CommonControllerTest {

    private final CommonController controller = new CommonController();

    @Test
    @DisplayName("健康检查返回 UP 状态与服务标识")
    void health_returnsUp() {
        Result<Map<String, Object>> result = controller.health();

        assertEquals(200, result.getCode());
        Map<String, Object> info = result.getData();
        assertEquals("UP", info.get("status"));
        assertEquals("BareZen-OA", info.get("service"));
        assertEquals("1.0.0", info.get("version"));
        assertTrue(info.containsKey("timestamp"));
    }
}