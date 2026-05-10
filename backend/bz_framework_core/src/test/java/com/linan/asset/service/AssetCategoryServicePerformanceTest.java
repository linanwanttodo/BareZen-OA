package com.linan.asset.service;

import com.linan.asset.entity.AssetCategory;
import com.linan.core.common.test.TestDataBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资产分类Service性能测试
 *
 * 测试Service在高并发场景下的性能表现
 *
 * @author BareZen Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("资产分类Service性能测试")
@Disabled("bz_framework_core 模块无 @SpringBootConfiguration，且并发性能测试不适合单测套件；需移入 bz_web_app 模块作为独立集成/性能测试")
class AssetCategoryServicePerformanceTest {

    @Autowired
    private IAssetCategoryService categoryService;

    private static final int BATCH_SIZE = 100;
    private static final int CONCURRENT_THREADS = 10;

    @Test
    @DisplayName("批量插入性能测试 - 插入100条记录")
    void batchInsert_performance_100records() {
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < BATCH_SIZE; i++) {
            AssetCategory category = TestDataBuilder.createTestCategory(
                    (long) (i + 1),
                    "性能测试分类" + i
            );
            categories.add(category);
        }

        long startTime = System.currentTimeMillis();
        
        for (AssetCategory category : categories) {
            try {
                categoryService.addCategory(category);
            } catch (Exception e) {
                // 忽略重复名称异常，专注于性能测试
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("批量插入 " + BATCH_SIZE + " 条记录耗时: " + duration + "ms");
        assertTrue(duration < 5000, "批量插入100条记录应在5秒内完成");
    }

    @Test
    @DisplayName("查询性能测试 - 查询100次")
    void query_performance_100queries() {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            categoryService.listCategories(null);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("查询100次耗时: " + duration + "ms");
        assertTrue(duration < 1000, "查询100次应在1秒内完成");
    }

    @Test
    @DisplayName("并发查询性能测试 - 10个线程并发查询")
    void concurrentQuery_performance_10threads() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < 10; j++) {
                    categoryService.listCategories(null);
                }
            }, executorService);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("10个线程并发查询100次（每线程10次）耗时: " + duration + "ms");
        assertTrue(duration < 2000, "并发查询应在2秒内完成");

        executorService.shutdown();
    }

    @Test
    @DisplayName("并发插入性能测试 - 10个线程并发插入")
    void concurrentInsert_performance_10threads() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < 10; j++) {
                    try {
                        AssetCategory category = TestDataBuilder.createTestCategory(
                                (long) (threadId * 10 + j + 1000),
                                "并发测试分类" + threadId + "-" + j
                        );
                        categoryService.addCategory(category);
                    } catch (Exception e) {
                        // 忽略重复名称异常
                    }
                }
            }, executorService);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("10个线程并发插入100条记录耗时: " + duration + "ms");
        assertTrue(duration < 5000, "并发插入应在5秒内完成");

        executorService.shutdown();
    }

    @Test
    @DisplayName("内存占用测试 - 插入大量数据")
    void memoryUsage_largeData() {
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // 插入大量数据
        for (int i = 0; i < 1000; i++) {
            try {
                AssetCategory category = TestDataBuilder.createTestCategory(
                        (long) (i + 10000),
                        "内存测试分类" + i
                );
                categoryService.addCategory(category);
            } catch (Exception e) {
                // 忽略异常
            }
        }

        long peakMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = peakMemory - initialMemory;

        System.out.println("插入1000条记录内存增加: " + (memoryIncrease / 1024 / 1024) + "MB");
        
        // 内存增加应该在合理范围内（不超过100MB）
        assertTrue(memoryIncrease < 100 * 1024 * 1024, "内存增加应在100MB以内");
    }
}