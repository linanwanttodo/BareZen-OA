package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.annotation.RateLimit;
import com.linan.core.common.api.PageParamGuard;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysFile;
import com.linan.system.service.ISysFileService;
import com.linan.web.controller.dto.UserFileDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 我的文件 Controller（用户前台 /api/app/v1/files）
 *
 * <p>员工自助上传与管理「我的文件」。与管理后台的文件管理接口
 * （/api/system/v1/files）分离：这里无需管理权限，仅要求登录；
 * 文件归属由服务端强制注入当前登录用户（写入 sys_file.create_by），
 * 接口不接收任何身份参数，前端无法伪造他人文件。</p>
 *
 * <p>上传复用管理端同一 Service 方法
 * （{@link ISysFileService#uploadFile}：后缀白名单、100MB 上限、MIME 校验、
 * 存储抽象落盘与物理清理），与管理端口径完全一致；
 * 用户端仅额外强制 scope=private（私有文件），不暴露管理端的 admin/public 范围切换。</p>
 *
 * <p>可见范围口径：用户端列表、下载、删除均以「上传人=当前登录用户」为唯一可见性条件，
 * 不包含他人上传的 public 文件（管理端 scope=public 是面向管理后台的浏览口径，
 * 其下载需 system:file:download 权限，不适用于用户前台）。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Tag(name = "我的文件", description = "用户前台 - 文件上传与个人文件管理")
@RestController
@RequestMapping("/api/app/v1/files")
@RequiredArgsConstructor
public class AppFileController {

    /** 用户端文件可见范围：private=仅本人可见（不进入管理端 admin/public 列表） */
    private static final String SCOPE_PRIVATE = "private";

    /** 文件夹记录不可下载/删除（用户端不提供文件夹能力） */
    private static final int FOLDER_FLAG = 1;

    private final ISysFileService fileService;

    /**
     * 上传文件（multipart/form-data）
     * POST /api/app/v1/files/upload
     *
     * <p>上传人由服务端强制取当前登录用户，接口不接收 uploader/userId 参数；
     * 文件大小与类型限制复用管理端同一 Service 校验，超限返回友好错误。</p>
     */
    @RateLimit(key = "app:file:upload", limit = 10, window = 60)
    @Operation(summary = "上传文件")
    @OperLog(title = "我的文件", businessType = 1)
    @PostMapping("/upload")
    public Result<UserFileDTO> upload(
            @Parameter(description = "上传文件") @RequestParam("file") MultipartFile file) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            // 归属与可见范围均由服务端决定：仅本人可见，且不接受前端传入文件夹/范围
            SysFile sysFile = fileService.uploadFile(file, 0L, SCOPE_PRIVATE, userId);
            return Result.success(toDTO(sysFile));
        } catch (IllegalArgumentException e) {
            log.warn("文件上传参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("文件上传IO异常", e);
            return Result.error("文件上传失败，请稍后重试");
        }
    }

    /**
     * 我的文件分页列表（上传人=当前登录用户，名称关键字筛选）
     * GET /api/app/v1/files
     *
     * <p>归属条件服务端强制注入，接口不接收 userId 参数；
     * 排序按创建时间倒序（最新上传在前）。</p>
     */
    @Operation(summary = "我的文件分页列表")
    @GetMapping
    public Result<PageResult<UserFileDTO>> myFiles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "文件名关键字") @RequestParam(required = false) String fileName) {

        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<SysFile>()
                // 归属强制取当前登录用户：create_by 记录上传者
                .eq(SysFile::getCreateBy, userId)
                // 用户端只管理文件，不展示文件夹
                .ne(SysFile::getIsFolder, FOLDER_FLAG);
        if (StringUtils.hasText(fileName)) {
            wrapper.like(SysFile::getOriginalName, fileName.trim());
        }
        wrapper.orderByDesc(SysFile::getCreateTime).orderByDesc(SysFile::getId);
        IPage<SysFile> result = fileService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result.convert(this::toDTO));
    }

    /**
     * 下载我的文件（仅本人文件可下载）
     * GET /api/app/v1/files/{id}/download
     *
     * <p>归属校验：仅上传者本人可下载；下载复用管理端同一 Service 方法
     * （流式返回 + Content-Disposition 带原始文件名），物理读取走存储抽象。</p>
     */
    @Operation(summary = "下载我的文件")
    @GetMapping("/{id}/download")
    public void download(@Parameter(description = "文件ID") @PathVariable Long id,
                         HttpServletResponse response) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        SysFile file = fileService.getById(id);
        if (file == null || !isOwner(file, userId)) {
            // 他人文件与不存在文件返回同一状态码，避免探测文件是否存在
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (file.getIsFolder() != null && file.getIsFolder() == FOLDER_FLAG) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        fileService.downloadFile(id, response);
    }

    /**
     * 删除我的文件（仅本人）
     * DELETE /api/app/v1/files/{id}
     *
     * <p>归属校验：仅上传者本人可删除；删除复用管理端同一 Service 方法
     * （逻辑删除记录 + 事务提交后清理本地存储物理文件）。</p>
     */
    @Operation(summary = "删除我的文件")
    @OperLog(title = "我的文件", businessType = 3)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "文件ID") @PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        SysFile file = fileService.getById(id);
        if (file == null || !isOwner(file, userId)) {
            return Result.error("文件不存在或无权操作");
        }
        boolean ok = fileService.deleteFile(id);
        return ok ? Result.success(null) : Result.error("删除失败，文件不存在");
    }

    /** 归属校验：仅上传者本人（create_by=当前登录用户） */
    private boolean isOwner(SysFile file, Long userId) {
        return file.getCreateBy() != null && userId.equals(file.getCreateBy());
    }

    /** 实体转公开视图：仅映射用户端可见字段，屏蔽存储路径与文件 URL */
    private UserFileDTO toDTO(SysFile file) {
        UserFileDTO dto = new UserFileDTO();
        dto.setId(file.getId());
        dto.setFileName(StringUtils.hasText(file.getOriginalName()) ? file.getOriginalName() : file.getFileName());
        dto.setFileSize(file.getFileSize());
        dto.setFileType(file.getFileType());
        dto.setStorageType(file.getStorageType());
        dto.setCreateTime(file.getCreateTime());
        return dto;
    }
}
