package com.linan.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysFile;
import com.linan.system.service.ISysFileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 文件管理控制器
 *
 * 注意：不继承 BaseController，避免 BaseController 的泛型 CRUD 路由（如 GET /{id}）
 * 与本控制器的自定义端点（/tree、/page-by-folder 等）产生冲突。
 */
@RestController
@RequestMapping("/api/system/v1/files")
public class SysFileController {

    @Autowired
    private ISysFileService fileService;

    // ============ 文件上传 ============

    @RequirePermission("system:file:upload")
    @OperLog(title = "文件管理", businessType = 1)
    @PostMapping("/upload")
    public Result<SysFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String scope) {
        try {
            SysFile sysFile = fileService.uploadFile(file, folderId, scope);
            return Result.success(sysFile);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    // ============ 下载 ============

    /** 无需登录，下载链接直接可访问 */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        fileService.downloadFile(id, response);
    }

    // ============ 删除（支持文件夹递归删除） ============

    @RequirePermission("system:file:delete")
    @OperLog(title = "文件管理", businessType = 3)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = fileService.deleteFile(id);
        return ok ? Result.success(null) : Result.error("删除失败，文件不存在");
    }

    // ============ 文件夹操作 ============

    @RequirePermission("system:file:add")
    @OperLog(title = "文件管理", businessType = 1)
    @PostMapping("/folder")
    public Result<SysFile> createFolder(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null
                ? Long.valueOf(body.get("parentId").toString()) : null;
        String scope = (String) body.get("scope");
        try {
            SysFile folder = fileService.createFolder(name, parentId, scope);
            return Result.success(folder);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @RequirePermission("system:file:edit")
    @OperLog(title = "文件管理", businessType = 2)
    @PutMapping("/rename/{id}")
    public Result<SysFile> rename(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object nameObj = body.get("name");
        String newName = nameObj != null ? nameObj.toString() : null;
        try {
            SysFile entity = fileService.rename(id, newName);
            return Result.success(entity);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @RequirePermission("system:file:list")
    @GetMapping("/tree")
    public Result<List<SysFile>> getTree(
            @RequestParam(required = false) Long parentId) {
        return Result.success(fileService.getFolderTree(parentId));
    }

    // ============ 文件/文件夹列表（按文件夹分页） ============

    /**
     * 获取指定文件夹下的内容（文件夹优先，含分页）
     * 管理端：不传 scope 则显示所有文件（admin + public）
     * 用户端：传 scope=public 只显示公开文件
     */
    @RequirePermission("system:file:list")
    @GetMapping("/page-by-folder")
    public Result<IPage<SysFile>> pageByFolder(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String originalName) {
        Page<SysFile> page = new Page<>(pageNum, pageSize);
        IPage<SysFile> result = fileService.pageByFolder(page, folderId, scope, fileType, originalName);
        return Result.success(result);
    }

    @RequirePermission("system:file:list")
    @GetMapping("/sub-folders")
    public Result<List<SysFile>> getSubFolders(
            @RequestParam(required = false) Long folderId) {
        return Result.success(fileService.getSubFolders(folderId));
    }
}
