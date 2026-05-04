package com.linan.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.annotation.RateLimit;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysFile;
import com.linan.system.entity.SysRole;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.mapper.SysRoleMapper;
import com.linan.system.service.ISysFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 文件管理控制器
 *
 * 注意：不继承 BaseController，避免 BaseController 的泛型 CRUD 路由（如 GET /{id}）
 * 与本控制器的自定义端点（/tree、/page-by-folder 等）产生冲突。
 */
@Slf4j
@Tag(name = "文件管理", description = "文件上传、下载、文件夹管理")
@RestController
@RequestMapping("/api/system/v1/files")
@RequiredArgsConstructor
public class SysFileController {

    private final ISysFileService fileService;

    private final SysRoleMapper roleMapper;

    private final SysMenuMapper menuMapper;

    // ============ 文件上传 ============

    @RateLimit(key = "system:file:upload", limit = 10, window = 60)
    @RequirePermission("system:file:upload")
    @Operation(summary = "上传文件")
    @OperLog(title = "文件管理", businessType = 1)
    @PostMapping("/upload")
    public Result<SysFile> upload(
            @Parameter(description = "上传文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "目标文件夹ID") @RequestParam(required = false) Long folderId,
            @Parameter(description = "文件范围(public/admin)") @RequestParam(required = false) String scope) {
        try {
            SysFile sysFile = fileService.uploadFile(file, folderId, scope);
            return Result.success(sysFile);
        } catch (IllegalArgumentException e) {
            log.warn("文件上传参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("文件上传IO异常", e);
            return Result.error("文件上传失败，请稍后重试");
        }
    }

    // ============ 下载 ============

    /**
     * 需登录下载。public 范围文件任意登录用户可下；admin 范围需文件管理权限。
     */
    @Operation(summary = "下载文件")
    @GetMapping("/download/{id}")
    public void download(@Parameter(description = "文件ID") @PathVariable Long id, HttpServletResponse response) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        SysFile file = fileService.getById(id);
        if (file == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // admin 私有文件：必须拥有 system:file:download 权限
        if (!"public".equalsIgnoreCase(file.getScope())) {
            List<String> perms = roleAndMenuCheck(userId);
            if (!perms.contains("system:file:download")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        fileService.downloadFile(id, response);
    }

    // ============ 删除（支持文件夹递归删除） ============

    @RequirePermission("system:file:delete")
    @Operation(summary = "删除文件")
    @OperLog(title = "文件管理", businessType = 3)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "文件ID") @PathVariable Long id) {
        boolean ok = fileService.deleteFile(id);
        return ok ? Result.success(null) : Result.error("删除失败，文件不存在");
    }

    // ============ 文件夹操作 ============

    @RequirePermission("system:file:add")
    @Operation(summary = "创建文件夹")
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
            log.warn("创建文件夹参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @RequirePermission("system:file:edit")
    @Operation(summary = "重命名文件/文件夹")
    @OperLog(title = "文件管理", businessType = 2)
    @PutMapping("/rename/{id}")
    public Result<SysFile> rename(@Parameter(description = "文件/文件夹ID") @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object nameObj = body.get("name");
        String newName = nameObj != null ? nameObj.toString() : null;
        try {
            SysFile entity = fileService.rename(id, newName);
            return Result.success(entity);
        } catch (IllegalArgumentException e) {
            log.warn("重命名参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @RequirePermission("system:file:list")
    @Operation(summary = "获取文件夹树")
    @GetMapping("/tree")
    public Result<List<SysFile>> getTree(
            @Parameter(description = "父文件夹ID") @RequestParam(required = false) Long parentId) {
        return Result.success(fileService.getFolderTree(parentId));
    }

    // ============ 文件/文件夹列表（按文件夹分页） ============

    /**
     * 获取指定文件夹下的内容（文件夹优先，含分页）
     * 管理端：不传 scope 则显示所有文件（admin + public）
     * 用户端：传 scope=public 只显示公开文件（任意登录用户）
     */
    @RequirePermission("system:file:list")
    @Operation(summary = "按文件夹分页查询文件")
    @GetMapping("/page-by-folder")
    public Result<PageResult<SysFile>> pageByFolder(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "文件夹ID") @RequestParam(required = false) Long folderId,
            @Parameter(description = "文件范围(public/admin)") @RequestParam(required = false) String scope,
            @Parameter(description = "文件类型") @RequestParam(required = false) String fileType,
            @Parameter(description = "原始文件名") @RequestParam(required = false) String originalName) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        // 非 public 浏览需要管理权限标识；无则强制 scope=public
        if (scope == null || scope.isBlank()) {
            // 保持管理端默认行为，但要求登录
            scope = null;
        } else if (!"public".equalsIgnoreCase(scope) && !"admin".equalsIgnoreCase(scope)) {
            scope = "public";
        }
        Page<SysFile> page = new Page<>(pageNum, pageSize);
        IPage<SysFile> result = fileService.pageByFolder(page, folderId, scope, fileType, originalName);
        return Result.page(result);
    }

    @RequirePermission("system:file:list")
    @Operation(summary = "获取子文件夹列表")
    @GetMapping("/sub-folders")
    public Result<List<SysFile>> getSubFolders(
            @Parameter(description = "父文件夹ID") @RequestParam(required = false) Long folderId) {
        if (AuthInterceptor.getCurrentUserId() == null) {
            return Result.error("未登录");
        }
        return Result.success(fileService.getSubFolders(folderId));
    }

    // ============ 内部辅助方法 ============

    /**
     * 查询当前用户的所有权限标识（用于 download 等 void 方法的内联权限校验）
     */
    private List<String> roleAndMenuCheck(Long userId) {
        List<SysRole> roles = roleMapper.selectRolesByUserId(userId);
        boolean isAdmin = roles.stream().anyMatch(r -> "admin".equals(r.getRoleKey()));
        if (isAdmin) {
            return List.of("system:file:download"); // admin 直接放行
        }
        Set<String> perms = new HashSet<>();
        for (SysRole role : roles) {
            perms.addAll(menuMapper.selectPermsByRoleId(role.getId()));
        }
        return List.copyOf(perms);
    }
}
