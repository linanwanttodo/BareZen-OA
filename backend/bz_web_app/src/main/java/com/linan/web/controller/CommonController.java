package com.linan.web.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.PassAuth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@PassAuth
@Tag(name = "公共接口", description = "健康检查等公共端点")
@RestController
@RequestMapping("/api/common/v1")
public class CommonController {

    @Operation(summary = "服务健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", "UP");
        info.put("timestamp", LocalDateTime.now().toString());
        info.put("service", "BareZen-OA");
        info.put("version", "1.0.0");
        return Result.success(info);
    }
}
