package com.linan.core.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * S3 兼容对象存储实现（MinIO / 阿里云 OSS S3 兼容端点 / 腾讯 COS）
 *
 * <p>配置 {@code barezen.storage.type=s3} 时启用；endpoint 指向兼容端点，
 * 未配置 accessKey/secretKey 时回退到 AWS 默认凭证链（环境变量/IAM 角色等）。</p>
 *
 * <p>约定：bucket 内 key 即业务相对路径（{@code yyyy/MM/dd/uuid.ext}），
 * 与 {@link LocalFileStorage} 一致，切换存储类型无需迁移数据（历史文件仍从本地读）。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "barezen.storage.type", havingValue = "s3")
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;

    private final String bucket;

    public S3FileStorage(
            @Value("${barezen.storage.s3.endpoint:}") String endpoint,
            @Value("${barezen.storage.s3.bucket:}") String bucket,
            @Value("${barezen.storage.s3.region:}") String region,
            @Value("${barezen.storage.s3.access-key:}") String accessKey,
            @Value("${barezen.storage.s3.secret-key:}") String secretKey,
            @Value("${barezen.storage.s3.path-style:true}") boolean pathStyle) {

        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("barezen.storage.s3.bucket 未配置，但 barezen.storage.type=s3");
        }

        software.amazon.awssdk.services.s3.S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region != null && !region.isBlank() ? region : "us-east-1"))
                .forcePathStyle(pathStyle);

        // 显式凭证优先；未配置时使用 AWS 默认凭证链（便于 IAM 角色场景）
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        }
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        this.s3Client = builder.build();
        this.bucket = bucket;
        log.info("S3 文件存储初始化完成: endpoint={}, bucket={}, region={}", endpoint, bucket, region);
    }

    /** 测试用：注入预构建 S3Client */
    S3FileStorage(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String getType() {
        return "s3";
    }

    @Override
    public void store(InputStream inputStream, long contentLength, String relativePath) throws IOException {
        String key = normalizeRelativePath(relativePath);
        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build(),
                    RequestBody.fromInputStream(inputStream, contentLength));
        } catch (S3Exception e) {
            throw new IOException("对象存储上传失败: key=" + key + ", status=" + e.statusCode(), e);
        }
    }

    @Override
    public InputStream load(String relativePath) throws IOException {
        String key = normalizeRelativePath(relativePath);
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new java.io.FileNotFoundException("对象不存在: " + key);
        } catch (S3Exception e) {
            throw new IOException("对象存储下载失败: key=" + key + ", status=" + e.statusCode(), e);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        try {
            String key = normalizeRelativePath(relativePath);
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (IllegalArgumentException e) {
            // 与 LocalFileStorage 保持一致：路径校验失败视为不存在
            return false;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.warn("对象存在性检查失败: key={}, status={}", relativePath, e.statusCode());
            return false;
        }
    }

    @Override
    public void delete(String relativePath) {
        String key = normalizeRelativePath(relativePath);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            log.warn("对象删除失败: key={}, status={}", key, e.statusCode());
        }
    }
}