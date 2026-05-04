package com.linan.system.controller;

import com.linan.core.common.api.Result;
import com.linan.core.gen.TableInfo;
import com.linan.core.security.RequirePermission;
import com.linan.system.service.IGenService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 代码生成控制器
 */
@Slf4j
@Tag(name = "代码生成", description = "数据库表查询与代码生成")
@RestController
@RequestMapping("/api/system/v1/gen")
@RequiredArgsConstructor
public class GenController {

    private final IGenService genService;

    /**
     * 查询业务表列表
     */
    @RequirePermission("system:gen:list")
    @Operation(summary = "查询业务表列表")
    @GetMapping("/tables")
    public Result<List<TableInfo>> listTables(@Parameter(description = "表名称(支持模糊搜索)") @RequestParam(required = false) String tableName) {
        List<TableInfo> tables = genService.selectTableList(tableName);
        return Result.success(tables);
    }

    /**
     * 查询表详情（含列信息）
     */
    @RequirePermission("system:gen:list")
    @Operation(summary = "查询表详情")
    @GetMapping("/table/{tableName}")
    public Result<TableInfo> getTable(@Parameter(description = "表名称") @PathVariable String tableName) {
        TableInfo table = genService.selectTableByName(tableName);
        if (table == null) {
            return Result.error("表 " + tableName + " 不存在");
        }
        return Result.success(table);
    }

    /**
     * 生成代码
     */
    @RequirePermission("system:gen:code")
    @Operation(summary = "生成代码")
    @PostMapping("/gen/{tableName}")
    public Result<Map<String, String>> generateCode(@Parameter(description = "表名称") @PathVariable String tableName) {
        try {
            Map<String, String> codeMap = genService.generateCode(tableName);
            return Result.success(codeMap);
        } catch (Exception e) {
            log.error("生成代码失败: tableName={}", tableName, e);
            return Result.error("生成代码失败，请稍后重试");
        }
    }
}
