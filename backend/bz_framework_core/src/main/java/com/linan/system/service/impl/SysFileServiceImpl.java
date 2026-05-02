package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysFile;
import com.linan.system.mapper.SysFileMapper;
import com.linan.system.service.ISysFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.commons.io.FilenameUtils;
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
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements ISysFileService {

    /** 文件存储根路径，默认 <user.dir>/uploads，可通过 barezen.upload-path 或环境变量 BAREZEN_UPLOAD_PATH 覆盖 */
    private final String basePath;

    /** 允许上传的文件后缀白名单（空=不限制） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "ico",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "csv",
            "mp4", "avi", "mov", "wmv", "flv", "mkv",
            "mp3", "wav", "wma", "flac", "aac", "ogg",
            "zip", "rar", "7z", "tar", "gz",
            "json", "xml", "yaml", "yml", "md"
    );

    /** 允许上传的最大文件大小（100 MB） */
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024L;

    public SysFileServiceImpl(@Value("${barezen.upload-path:#{systemProperties['user.dir'] + '/uploads'}}") String uploadPath) {
        // 如果配置的是相对路径，基于 user.dir 解析
        if (uploadPath.startsWith("./") || uploadPath.startsWith("../") || uploadPath.equals(".")) {
            this.basePath = new File(System.getProperty("user.dir"), uploadPath).getAbsolutePath() + "/";
        } else if (!uploadPath.endsWith("/")) {
            this.basePath = uploadPath + "/";
        } else {
            this.basePath = uploadPath;
        }
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
                        } else if ("svg".equals(suffix) && !"image/svg+xml".equals(detectedMime)) {
                            throw new IllegalArgumentException("文件内容与扩展名不匹配");
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
            String dirPath = basePath + datePath;
            File dir = new File(dirPath);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("创建上传目录失败: " + dirPath);
            }

            File dest = new File(dirPath + "/" + fileName);
            Files.move(tempFile, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 存储相对路径，避免项目迁移或部署路径变化导致文件无法访问
            String relativePath = datePath + "/" + fileName;

            SysFile sysFile = new SysFile();
            sysFile.setFileName(fileName);
            sysFile.setOriginalName(originalName != null ? originalName : "unknown");
            sysFile.setFilePath(relativePath);
            sysFile.setFileSize(file.getSize());
            sysFile.setFileType(detectFileType(suffix));
            sysFile.setStorageType("local");
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

        // 用 basePath 拼出完整磁盘路径
        File file = new File(basePath + sysFile.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("文件已丢失");
        }

        try (InputStream is = new FileInputStream(file);
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
    public boolean deleteFile(Long id) {
        SysFile sysFile = this.getById(id);
        if (sysFile == null) {
            return false;
        }

        // 如果是文件夹，递归删除所有子文件/子文件夹
        if (sysFile.getIsFolder() != null && sysFile.getIsFolder() == 1) {
            deleteChildrenRecursively(id);
        }

        // 删除物理文件（仅文件）
        if ((sysFile.getIsFolder() == null || sysFile.getIsFolder() == 0)
                && StringUtils.hasText(sysFile.getFilePath())) {
            File file = new File(basePath + sysFile.getFilePath());
            if (file.exists() && !file.delete()) {
                log.warn("物理文件删除失败: {}", file.getAbsolutePath());
            }
        }

        return this.removeById(id);
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

    /** 递归删除文件夹下所有子文件/子文件夹 */
    private void deleteChildrenRecursively(Long folderId) {
        List<SysFile> children = this.list(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getFolderId, folderId)
                .eq(SysFile::getDelFlag, 0));
        for (SysFile child : children) {
            if (child.getIsFolder() != null && child.getIsFolder() == 1) {
                deleteChildrenRecursively(child.getId());
            }
            // 删除物理文件
            if ((child.getIsFolder() == null || child.getIsFolder() == 0)
                    && StringUtils.hasText(child.getFilePath())) {
                File file = new File(basePath + child.getFilePath());
                if (file.exists()) {
                    file.delete();
                }
            }
            this.removeById(child.getId());
        }
    }

    /** 根据后缀检测文件类型 */
    private String detectFileType(String suffix) {
        if (!StringUtils.hasText(suffix)) return "other";
        String s = suffix.toLowerCase();
        if (s.matches("(jpg|jpeg|png|gif|webp|svg|bmp|ico)")) return "image";
        if (s.matches("(doc|docx|xls|xlsx|ppt|pptx|pdf|txt|csv)")) return "document";
        if (s.matches("(mp4|avi|mov|wmv|flv|mkv)")) return "video";
        if (s.matches("(mp3|wav|wma|flac|aac|ogg)")) return "audio";
        return "other";
    }
}
