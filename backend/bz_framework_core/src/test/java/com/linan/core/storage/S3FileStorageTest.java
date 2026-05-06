package com.linan.core.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S3 兼容对象存储实现单元测试（mock S3Client，不依赖真实对象存储）
 *
 * <p>覆盖：putObject/getObject/headObject/deleteObject 调用与参数（bucket/key 透传）、
 * 对象不存在语义、路径穿越防御。</p>
 */
class S3FileStorageTest {

    private final S3Client s3Client = mock(S3Client.class);

    private S3FileStorage storage() {
        return new S3FileStorage(s3Client, "barezen-bucket");
    }

    @Test
    @DisplayName("store 调用 putObject 且 key 为相对路径")
    void store_putsObjectWithKey() throws IOException {
        byte[] content = "hello".getBytes();

        storage().store(new ByteArrayInputStream(content), content.length, "2026/08/05/uuid.txt");

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("load 调用 getObject 并透传流内容")
    void load_returnsObjectStream() throws IOException {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                        GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream("data".getBytes()),
                                mock(software.amazon.awssdk.http.Abortable.class))));
        S3FileStorage storage = storage();

        try (InputStream is = storage.load("2026/08/05/uuid.txt")) {
            assertArrayEquals("data".getBytes(), is.readAllBytes());
        }
        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("load 对象不存在抛 FileNotFoundException")
    void load_missingKey_throws() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());
        S3FileStorage storage = storage();

        assertThrows(java.io.FileNotFoundException.class, () -> storage.load("nope.txt"));
    }

    @Test
    @DisplayName("exists：headObject 成功为 true，NoSuchKey 为 false")
    void exists_headObjectResult() {
        S3FileStorage storage = storage();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(null);
        assertTrue(storage.exists("a.txt"));

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());
        assertFalse(storage.exists("missing.txt"));
    }

    @Test
    @DisplayName("delete 调用 deleteObject 且幂等（异常不抛出）")
    void delete_callsDeleteObject() {
        S3FileStorage storage = storage();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(null);

        storage.delete("a.txt");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("路径穿越（..）被拒绝")
    void pathTraversal_rejected() {
        S3FileStorage storage = storage();

        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new ByteArrayInputStream(new byte[]{1}), 1, "../escape"));
        assertFalse(storage.exists("..\\x"));
    }

    @Test
    @DisplayName("getType 返回 s3")
    void type_isS3() {
        assertEquals("s3", storage().getType());
    }
}