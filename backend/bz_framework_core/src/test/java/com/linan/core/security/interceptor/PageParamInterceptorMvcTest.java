package com.linan.core.security.interceptor;

import com.linan.core.common.api.Result;
import com.linan.core.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * PageParamInterceptor 端到端链路测试
 *
 * <p>单元级测试只能证明拦截器抛了异常，无法证明"异常经 GlobalExceptionHandler 渲染成
 * 统一 {@code Result} 响应体"这条链路成立。此处用 standalone MockMvc 在不拉起 Spring
 * 上下文的前提下跑通完整分发链（含 preHandle 抛异常的解析路径）。</p>
 */
class PageParamInterceptorMvcTest {

    /** 探针 Controller：不自行校验分页参数，用于验证拦截器独立生效 */
    @RestController
    static class ProbeController {
        @GetMapping("/api/probe/page")
        public Result<String> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
            return Result.success("ok");
        }
    }

    private MockMvc mockMvc() {
        return standaloneSetup(new ProbeController())
                .addInterceptors(new PageParamInterceptor())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("超出硬上限的 pageSize 被拒并渲染为 code=400 标准响应体")
    void aboveHardCap_rendersUnifiedErrorBody() throws Exception {
        mockMvc().perform(get("/api/probe/page").param("pageSize", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("分页参数不合法"));
    }

    @Test
    @DisplayName("硬上限内的 pageSize 正常放行到 Controller")
    void withinHardCap_reachesController() throws Exception {
        mockMvc().perform(get("/api/probe/page").param("pageSize", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    @DisplayName("非数字 pageSize 同样按非法拒绝，不落 Controller 的类型转换错误")
    void nonNumeric_rejected() throws Exception {
        mockMvc().perform(get("/api/probe/page").param("pageSize", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("分页参数不合法"));
    }
}
