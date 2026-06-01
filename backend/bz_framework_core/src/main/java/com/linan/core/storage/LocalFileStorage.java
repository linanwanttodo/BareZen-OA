package com.linan.core.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘文件存储实现（默认）
 *
 * <p>文件保存在 {@code barezen.upload-path}（默认 {@code ./uploads}）下，
 * 按相对路径（{@code yyyy/MM/dd/uuid.ext}）组织目录。相对路径禁止包含
 * {@code ..} / 绝对路径（见 {@link FileStorage#normalizeRelativePath}）。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "barezen.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    /** 存储根目录（绝对路径，末尾带 /） */
    private final String basePath;

    public LocalFileStorage(@Value("${barezen.upload-path:#{systemProperties['user.dir'] + '/uploads'}}") String uploadPath) {
        this.basePath = FileStorage.resolveBasePath(uploadPath);
    }

    @Override
    public String getType() {
        return "local";
    }

    @Override
    public void store(InputStream inputStream, long contentLength, String relativePath) throws IOException {
        String path = normalizeRelativePath(relativePath);
        File dest = new File(basePath + path);
        File dir = dest.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("创建存储目录失败: " + dir.getAbsolutePath());
        }
        // REPLACE_EXISTING：UUID 文件名理论不冲突，幂等覆盖更安全（重传场景）
        Files.copy(inputStream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        log.debug("本地存储文件: {}", dest.getAbsolutePath());
    }

    @Override
    public InputStream load(String relativePath) throws IOException {
        String path = normalizeRelativePath(relativePath);
        File file = new File(basePath + path);
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("文件不存在: " + path);
        }
        return new FileInputStream(file);
    }

    @Override
    public boolean exists(String relativePath) {
        try {
            return Files.isRegularFile(Path.of(basePath + normalizeRelativePath(relativePath)));
        } catch (IllegalArgumentException e) {
            log.warn("路径校验失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(Path.of(basePath + normalizeRelativePath(relativePath)));
        } catch (IllegalArgumentException e) {
            log.warn("路径校验失败，跳过删除: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("物理文件删除失败: {}", basePath + relativePath);
        }
    }
}