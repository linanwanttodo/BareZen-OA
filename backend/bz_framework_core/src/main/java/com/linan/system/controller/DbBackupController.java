package com.linan.system.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.annotation.RateLimit;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.service.IDbBackupService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 数据库备份 Controller
 *
 * @author BareZen Team
 */
@Tag(name = "数据备份", description = "系统监控 - 数据库备份管理")
@Slf4j
@RestController
@RequestMapping("/api/system/v1/backup")
@RequiredArgsConstructor
public class DbBackupController {

    private final IDbBackupService backupService;

    /**
     * 创建备份
     */
    @RateLimit(key = "system:backup:create", limit = 3, window = 60)
    @OperLog(title = "数据库备份", businessType = 1)
    @RequirePermission("system:backup:add")
    @Operation(summary = "创建数据库备份")
    @PostMapping("/create")
    public Result<Map<String, Object>> createBackup() {
        Map<String, Object> result = backupService.createBackup();
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error(result.getOrDefault("message", "备份失败").toString());
    }

    /**
     * 备份列表
     */
    @RequirePermission("system:backup:list")
    @Operation(summary = "查询备份列表")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listBackups() {
        return Result.success(backupService.listBackups());
    }

    /** 文件名正则：仅允许安全的文件名格式，防止路径遍历 */
    private static final java.util.regex.Pattern FILENAME_PATTERN =
            java.util.regex.Pattern.compile("^[\\w\\-]+\\.sql$");

    /**
     * 下载备份文件
     */
    @RequirePermission("system:backup:list")
    @Operation(summary = "下载数据库备份")
    @GetMapping("/download/{filename}")
    public void download(@PathVariable String filename, HttpServletResponse response) {
        // 路径遍历防护：校验文件名格式
        if (filename == null || !FILENAME_PATTERN.matcher(filename).matches()) {
            response.setStatus(404);
            return;
        }

        String filePath = backupService.getBackupPath(filename);
        if (filePath == null) {
            response.setStatus(404);
            return;
        }

        File file = new File(filePath);
        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            response.setContentLengthLong(file.length());

            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (Exception e) {
            log.error("下载备份文件失败: {}", filename, e);
            response.setStatus(500);
        }
    }

    /**
     * 删除备份
     */
    @OperLog(title = "数据库备份", businessType = 3)
    @RequirePermission("system:backup:delete")
    @Operation(summary = "删除数据库备份")
    @DeleteMapping("/{filename}")
    public Result<Void> delete(@PathVariable String filename) {
        return backupService.deleteBackup(filename)
                ? Result.success(null)
                : Result.error("删除失败");
    }
}
