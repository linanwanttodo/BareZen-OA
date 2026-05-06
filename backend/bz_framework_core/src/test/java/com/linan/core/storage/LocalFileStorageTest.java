package com.linan.core.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地磁盘文件存储实现单元测试
 *
 * <p>使用 JUnit {@code @TempDir} 隔离真实文件系统，覆盖 store/load/exists/delete
 * 全生命周期与路径穿越防御。</p>
 */
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage() {
        return new LocalFileStorage(tempDir.toString());
    }

    @Test
    @DisplayName("store 后 load 返回相同内容")
    void storeThenLoad_roundTrip() throws IOException {
        LocalFileStorage storage = storage();
        byte[] content = "hello barezen".getBytes(StandardCharsets.UTF_8);

        storage.store(new ByteArrayInputStream(content), content.length, "2026/08/05/uuid.jpg");

        assertTrue(storage.exists("2026/08/05/uuid.jpg"));
        try (InputStream is = storage.load("2026/08/05/uuid.jpg")) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    @Test
    @DisplayName("store 自动创建多级目录")
    void store_createsNestedDirs() throws IOException {
        LocalFileStorage storage = storage();
        byte[] content = "x".getBytes(StandardCharsets.UTF_8);

        storage.store(new ByteArrayInputStream(content), 1, "2026/08/05/uuid.txt");

        assertTrue(Files.isRegularFile(tempDir.resolve("2026/08/05/uuid.txt")));
    }

    @Test
    @DisplayName("store 覆盖已存在文件（幂等重传）")
    void store_overwritesExisting() throws IOException {
        LocalFileStorage storage = storage();
        storage.store(new ByteArrayInputStream("v1".getBytes(StandardCharsets.UTF_8)), 2, "a.txt");

        storage.store(new ByteArrayInputStream("v2-long".getBytes(StandardCharsets.UTF_8)), 7, "a.txt");

        try (InputStream is = storage.load("a.txt")) {
            assertArrayEquals("v2-long".getBytes(StandardCharsets.UTF_8), is.readAllBytes());
        }
    }

    @Test
    @DisplayName("load 不存在的文件抛 FileNotFoundException")
    void load_missing_throws() {
        LocalFileStorage storage = storage();

        assertThrows(java.io.FileNotFoundException.class, () -> storage.load("not/exist.txt"));
    }

    @Test
    @DisplayName("delete 删除后 exists 为 false，重复删除静默成功")
    void delete_removesAndIdempotent() throws IOException {
        LocalFileStorage storage = storage();
        storage.store(new ByteArrayInputStream(new byte[]{1}), 1, "del.txt");

        storage.delete("del.txt");

        assertFalse(storage.exists("del.txt"));
        storage.delete("del.txt"); // 幂等：不抛异常
    }

    @Test
    @DisplayName("路径穿越（..）被拒绝")
    void pathTraversal_rejected() {
        LocalFileStorage storage = storage();

        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new ByteArrayInputStream(new byte[]{1}), 1, "../../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> storage.load("/etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> storage.load("..\\win.ini"));
        assertFalse(storage.exists("../escape.txt"));
    }

    @Test
    @DisplayName("getType 返回 local")
    void type_isLocal() {
        assertTrue("local".equals(storage().getType()));
    }
}