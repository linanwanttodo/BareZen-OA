package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.storage.FileStorage;
import com.linan.system.entity.SysFile;
import com.linan.system.mapper.SysFileMapper;
import com.linan.system.service.ISysFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements ISysFileService {

    /** 文件存储抽象（local=本地磁盘 / s3=对象存储），由 barezen.storage.type 决定 */
    private final FileStorage storage;

    /** 允许上传的文件后缀白名单（空=不限制）
     * 注意：故意排除 svg/html 等可内联执行脚本的类型——上传目录经 /profile 静态映射
     * 按原 content-type 直接输出，SVG 被直接导航时可在同源执行脚本（存储型 XSS）。
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "ico",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "csv",
            "mp4", "avi", "mov", "wmv", "flv", "mkv",
            "mp3", "wav", "wma", "flac", "aac", "ogg",
            "zip", "rar", "7z", "tar", "gz",
            "json", "xml", "yaml", "yml", "md"
    );

    /** 允许上传的最大文件大小（100 MB） */
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024L;

    public SysFileServiceImpl(FileStorage storage) {
        this.storage = storage;
    }

    @Override
    public SysFile uploadFile(MultipartFile file, Long folderId, String scope) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 100MB");
        }

        String originalName = file.getOriginalFilename();
        // 使用 FilenameUtils.getExtension() 安全提取后缀，避免 lastIndexOf 绕过问题
        String suffix = originalName != null
                ? FilenameUtils.getExtension(originalName).toLowerCase()
                : "";

        // 校验文件后缀白名单
        if (!suffix.isEmpty() && !ALLOWED_EXTENSIONS.contains(suffix)) {
            throw new IllegalArgumentException("不支持的文件类型: ." + suffix);
        }

        // 先将上传内容写入临时文件（MultipartFile 的流只能消费一次，
        // 不能先 transferTo 做 MIME 检测再次 transferTo 保存），
        // MIME 检测通过后直接将临时文件移动到目标位置
        Path tempFile = Files.createTempFile("upload-", suffix.isEmpty() ? ".tmp" : "." + suffix);
        try {
            file.transferTo(tempFile.toFile());

            // 校验文件内容 MIME 类型（防止魔数篡改）
            if (!suffix.isEmpty()) {
                try {
                    String detectedMime = Files.probeContentType(tempFile);
                    if (detectedMime != null) {
                        // 检查 MIME 类型是否与后缀匹配（图片类型验证）
                        if (suffix.matches("jpg|jpeg|png|gif|webp|bmp|ico")) {
                            if (!detectedMime.startsWith("image/")) {
                                throw new IllegalArgumentException("文件内容与扩展名不匹配，检测到 MIME: " + detectedMime);
                            }
                        }
                    }
                } catch (IOException e) {
                    // MIME 检测失败时仅记录日志，不阻断上传
                    log.warn("MIME 类型检测失败: {}", e.getMessage());
                }
            }

            String ext = suffix.isEmpty() ? "" : "." + suffix;
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            // 存储相对路径，避免项目迁移或部署路径变化导致文件无法访问；
            // 物理写入由 FileStorage 抽象完成（local 本地磁盘 / s3 对象存储）
            String relativePath = datePath + "/" + fileName;
            try (InputStream is = new FileInputStream(tempFile.toFile())) {
                storage.store(is, file.getSize(), relativePath);
            }

            SysFile sysFile = new SysFile();
            sysFile.setFileName(fileName);
            sysFile.setOriginalName(originalName != null ? originalName : "unknown");
            sysFile.setFilePath(relativePath);
            sysFile.setFileSize(file.getSize());
            sysFile.setFileType(detectFileType(suffix));
            sysFile.setStorageType(storage.getType());
            sysFile.setIsFolder(0);
            sysFile.setFolderId(folderId != null ? folderId : 0L);
            sysFile.setScope(StringUtils.hasText(scope) ? scope : "admin");
            sysFile.setSortOrder(0);
            this.save(sysFile);

            // save 后获取 ID，构造访问 URL
            sysFile.setFileUrl("/api/system/v1/files/download/" + sysFile.getId());
            this.updateById(sysFile);

            return sysFile;
        } finally {
            // 正常流程中临时文件已被 move 走，这里仅清理异常退出时的残留
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public void downloadFile(Long id, HttpServletResponse response) {
        SysFile sysFile = this.getById(id);
        if (sysFile == null) {
            throw new RuntimeException("文件不存在");
        }
        if (sysFile.getIsFolder() != null && sysFile.getIsFolder() == 1) {
            throw new RuntimeException("文件夹不可下载");
        }

        // 路径遍历防御：filePath 虽由服务端生成（yyyy/MM/dd + UUID），
        // 但为防数据库被篡改后拼接出任意路径，仍拒绝包含 .. 的路径
        String relativePath = sysFile.getFilePath();
        if (!StringUtils.hasText(relativePath)
                || relativePath.contains("..")
                || relativePath.startsWith("/")
                || relativePath.contains("\\")) {
            throw new RuntimeException("非法文件路径");
        }

        try (InputStream is = storage.load(relativePath);
             OutputStream os = response.getOutputStream()) {

            response.setContentType("application/octet-stream");
            String downloadFileName = URLEncoder.encode(
                    StringUtils.hasText(sysFile.getOriginalName())
                            ? sysFile.getOriginalName()
                            : sysFile.getFileName(),
                    StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + downloadFileName);
            response.setContentLengthLong(sysFile.getFileSize());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(Long id) {
        SysFile sysFile = this.getById(id);
        if (sysFile == null) {
            return false;
        }

        // 事务内先完成全部 DB 删除（失败整体回滚），物理文件路径先收集、
        // 记录删除成功后再清理磁盘，避免出现"记录还在、文件已丢"的悬挂数据
        List<String> physicalPaths = new ArrayList<>();
        collectPhysicalPaths(sysFile, physicalPaths);
        if (sysFile.getIsFolder() != null && sysFile.getIsFolder() == 1) {
            removeChildrenRecursively(sysFile.getId(), physicalPaths);
        }
        boolean removed = this.removeById(id);
        if (removed && !physicalPaths.isEmpty()) {
            deletePhysicalAfterCommit(physicalPaths);
        }
        return removed;
    }

    /**
     * 在事务提交成功后再删除物理文件。
     *
     * <p>避免在 {@code @Transactional} 方法内部（提交前）删磁盘：一旦后续 DB 操作回滚，
     * 文件已被删但记录仍在，形成悬挂数据。注册 afterCommit 回调保证"仅提交成功才删文件"，
     * 单个文件删除失败仅告警、不影响主流程。无活动事务同步时（如单元直调）立即执行。</p>
     */
    private void deletePhysicalAfterCommit(List<String> physicalPaths) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deletePhysicalFiles(physicalPaths);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deletePhysicalFiles(physicalPaths);
            }
        });
    }

    /** 逐个删除物理文件，单个失败仅记录告警，不影响其余文件与主流程 */
    private void deletePhysicalFiles(List<String> physicalPaths) {
        for (String path : physicalPaths) {
            try {
                storage.delete(path);
            } catch (Exception e) {
                log.warn("删除物理文件失败: {}", path, e);
            }
        }
    }

    /** 收集文件节点对应的物理存储路径（文件夹本身无物理文件） */
    private void collectPhysicalPaths(SysFile node, List<String> paths) {
        if ((node.getIsFolder() == null || node.getIsFolder() == 0)
                && StringUtils.hasText(node.getFilePath())) {
            paths.add(node.getFilePath());
        }
    }

    @Override
    public SysFile createFolder(String folderName, Long parentId, String scope) {
        if (!StringUtils.hasText(folderName)) {
            throw new IllegalArgumentException("文件夹名称不能为空");
        }

        SysFile folder = new SysFile();
        folder.setFileName(folderName);
        folder.setOriginalName(folderName);
        folder.setIsFolder(1);
        folder.setFolderId(parentId != null ? parentId : 0L);
        folder.setScope(StringUtils.hasText(scope) ? scope : "admin");
        folder.setFileType("folder");
        folder.setStorageType("local");
        folder.setSortOrder(0);
        this.save(folder);
        return folder;
    }

    @Override
    public SysFile rename(Long id, String newName) {
        if (!StringUtils.hasText(newName)) {
            throw new IllegalArgumentException("名称不能为空");
        }
        SysFile entity = this.getById(id);
        if (entity == null) {
            throw new RuntimeException("文件或文件夹不存在");
        }
        entity.setFileName(newName);
        entity.setOriginalName(newName);
        this.updateById(entity);
        return entity;
    }

    @Override
    public List<SysFile> getFolderTree(Long parentId) {
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFile::getIsFolder, 1)
                .eq(SysFile::getDelFlag, 0)
                .orderByAsc(SysFile::getSortOrder)
                .orderByAsc(SysFile::getCreateTime);
        long pid = parentId != null ? parentId : 0L;
        wrapper.eq(SysFile::getFolderId, pid);
        return this.list(wrapper);
    }

    @Override
    public IPage<SysFile> pageByFolder(Page<SysFile> page, Long folderId, String scope,
                                       String fileType, String originalName) {
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFile::getDelFlag, 0);

        // 按父文件夹过滤
        long pid = folderId != null ? folderId : 0L;
        wrapper.eq(SysFile::getFolderId, pid);

        // 按可见范围过滤
        if (StringUtils.hasText(scope)) {
            wrapper.eq(SysFile::getScope, scope);
        }

        // 按文件类型过滤
        if (StringUtils.hasText(fileType)) {
            if ("other".equals(fileType)) {
                // "其它" = 不是 image/document/video/audio/folder 的
                wrapper.notIn(SysFile::getFileType, "image", "document", "video", "audio", "folder");
            } else {
                wrapper.eq(SysFile::getFileType, fileType);
            }
        }

        // 按文件名搜索
        if (StringUtils.hasText(originalName)) {
            wrapper.like(SysFile::getOriginalName, originalName);
        }

        // 排序：文件夹优先，再按 sort_order，再按创建时间倒序
        wrapper.orderByDesc(SysFile::getIsFolder)
                .orderByAsc(SysFile::getSortOrder)
                .orderByDesc(SysFile::getCreateTime);

        return this.page(page, wrapper);
    }

    @Override
    public List<SysFile> getSubFolders(Long folderId) {
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFile::getIsFolder, 1)
                .eq(SysFile::getDelFlag, 0)
                .eq(SysFile::getFolderId, folderId != null ? folderId : 0L)
                .orderByAsc(SysFile::getSortOrder)
                .orderByAsc(SysFile::getCreateTime);
        return this.list(wrapper);
    }

    // ==================== 私有方法 ====================

    /** 递归删除文件夹下所有子文件/子文件夹（仅 DB 记录，物理路径收集后统一清理） */
    private void removeChildrenRecursively(Long folderId, List<String> physicalPaths) {
        List<SysFile> children = this.list(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getFolderId, folderId)
                .eq(SysFile::getDelFlag, 0));
        for (SysFile child : children) {
            if (child.getIsFolder() != null && child.getIsFolder() == 1) {
                removeChildrenRecursively(child.getId(), physicalPaths);
            }
            collectPhysicalPaths(child, physicalPaths);
            this.removeById(child.getId());
        }
    }

    /** 根据后缀检测文件类型 */
    private String detectFileType(String suffix) {
        if (!StringUtils.hasText(suffix)) return "other";
        String s = suffix.toLowerCase();
        if (s.matches("(jpg|jpeg|png|gif|webp|bmp|ico)")) return "image";
        if (s.matches("(doc|docx|xls|xlsx|ppt|pptx|pdf|txt|csv)")) return "document";
        if (s.matches("(mp4|avi|mov|wmv|flv|mkv)")) return "video";
        if (s.matches("(mp3|wav|wma|flac|aac|ogg)")) return "audio";
        return "other";
    }
}
