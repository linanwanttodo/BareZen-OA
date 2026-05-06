package com.linan.core.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件存储抽象（对象存储适配层）
 *
 * <p>将物理存储与业务解耦：业务层只感知相对路径（如 {@code 2026/08/05/uuid.jpg}），
 * 实际存储由实现决定（本地磁盘 / S3 兼容对象存储）。当前实现：</p>
 * <ul>
 *   <li>{@link LocalFileStorage}：本地磁盘（默认，配置 {@code barezen.storage.type=local}）</li>
 *   <li>{@link S3FileStorage}：S3 兼容对象存储（MinIO / 阿里云 OSS S3 兼容端点，配置 {@code barezen.storage.type=s3}）</li>
 * </ul>
 *
 * @author BareZen Team
 */
public interface FileStorage {

    /**
     * 存储类型标识，写入 {@code sys_file.storage_type}（local / s3），供下载端选择读取方式
     */
    String getType();

    /**
     * 保存文件内容到指定相对路径
     *
     * @param inputStream   文件内容（调用方负责关闭）
     * @param contentLength 内容字节数（对象存储分片/签名需要；本地实现忽略）
     * @param relativePath  相对路径（如 {@code 2026/08/05/uuid.jpg}），禁止包含 {@code ..} 或绝对路径
     */
    void store(InputStream inputStream, long contentLength, String relativePath) throws IOException;

    /**
     * 读取文件内容
     *
     * @return 文件流（调用方负责关闭）；文件不存在时抛 {@link java.io.FileNotFoundException}
     */
    InputStream load(String relativePath) throws IOException;

    /**
     * 判断文件是否存在
     */
    boolean exists(String relativePath);

    /**
     * 删除文件（文件不存在时静默成功）
     */
    void delete(String relativePath);

    /**
     * 相对路径安全校验：拒绝绝对路径、路径穿越（{@code ..}）与 Windows 分隔符。
     * 相对路径由服务端生成（yyyy/MM/dd + UUID），此处为防数据库被篡改后拼接出任意路径的兜底。
     */
    default String normalizeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("非法文件路径：路径为空");
        }
        if (relativePath.contains("..") || relativePath.startsWith("/") || relativePath.contains("\\")) {
            throw new IllegalArgumentException("非法文件路径：" + relativePath);
        }
        return relativePath;
    }
}