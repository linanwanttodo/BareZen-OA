package com.linan.plugin.masterdata;

import com.linan.plugin.masterdata.service.IDocNumberService;
import com.linan.web.BareZenApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单据号服务集成测试（真实 MySQL barezen_test 库，Flyway 自动建表）
 *
 * <p>核心验证：多线程并发调用 nextNumber 不重号（FOR UPDATE 行锁 + 乐观护栏）。
 * 该测试必须脱离 Spring 测试事务（不标 @Transactional），让每个发号调用
 * 在独立事务里持有真实行锁，才能验证并发语义。</p>
 */
@SpringBootTest(classes = BareZenApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("单据号服务集成测试（并发不重号）")
class DocNumberServiceConcurrencyTest {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^CT\\d{8}-\\d{4,}$");
    private static final String TEST_BIZ_KEY = "CNCTEST";
    private static final int THREADS = 20;
    private static final int NUMBERS_PER_THREAD = 25;

    @Autowired
    private IDocNumberService docNumberService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private void resetRule(String bizKey, String prefix, long currentSeq, String lastDate) {
        jdbcTemplate.update("DELETE FROM sys_doc_number_rule WHERE biz_key = ?", bizKey);
        jdbcTemplate.update(
                "INSERT INTO sys_doc_number_rule (biz_key, prefix, date_format, current_seq, step, last_date) "
                        + "VALUES (?, ?, 'yyyyMMdd', ?, 1, ?)",
                bizKey, prefix, currentSeq, lastDate);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM sys_doc_number_rule WHERE biz_key = ?", TEST_BIZ_KEY);
    }

    @Test
    @DisplayName("20 线程 x 25 次并发发号：500 个单据号全部唯一")
    void concurrentNextNumbers_allUnique() throws Exception {
        resetRule(TEST_BIZ_KEY, "CT", 0, null);

        ConcurrentLinkedQueue<String> numbers = new ConcurrentLinkedQueue<>();
        AtomicInteger failures = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int j = 0; j < NUMBERS_PER_THREAD; j++) {
                        numbers.add(docNumberService.nextNumber(TEST_BIZ_KEY));
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }
        ready.await();
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "并发发号未在限时内完成");

        assertEquals(0, failures.get(), "并发发号出现异常（含乐观护栏拒绝）");
        assertEquals(THREADS * NUMBERS_PER_THREAD, numbers.size(), "发号数量不足");
        Set<String> distinct = Set.copyOf(numbers);
        assertEquals(THREADS * NUMBERS_PER_THREAD, distinct.size(), "单据号出现重复");

        String today = LocalDate.now().format(DAY);
        assertTrue(numbers.stream().allMatch(n -> n.startsWith("CT" + today + "-")
                && NUMBER_PATTERN.matcher(n).matches()), "单据号格式不符: " + numbers.stream().findFirst());
    }

    @Test
    @DisplayName("顺序发号与跨日重置：0001 -> 0002，重置 last_date 后回到 0001")
    void sequentialAndDateReset() {
        resetRule(TEST_BIZ_KEY, "CT", 0, null);

        String today = LocalDate.now().format(DAY);
        assertEquals("CT" + today + "-0001", docNumberService.nextNumber(TEST_BIZ_KEY));
        assertEquals("CT" + today + "-0002", docNumberService.nextNumber(TEST_BIZ_KEY));

        jdbcTemplate.update("UPDATE sys_doc_number_rule SET last_date = '20200101' WHERE biz_key = ?",
                TEST_BIZ_KEY);
        assertEquals("CT" + today + "-0001", docNumberService.nextNumber(TEST_BIZ_KEY));
    }

    @Test
    @DisplayName("种子规则可用：SUP 规则发号格式为 SUPyyyyMMdd-NNNN")
    void seededSupplierRuleUsable() {
        String number = docNumberService.nextNumber("SUP");
        String today = LocalDate.now().format(DAY);
        assertTrue(number.startsWith("SUP" + today + "-"),
                "种子 SUP 规则发号格式不符: " + number);
        assertTrue(number.matches("^SUP\\d{8}-\\d{4,}$"));
    }

    @Test
    @DisplayName("发号规则不存在抛业务异常")
    void ruleMissing_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(
                com.linan.core.common.exception.BusinessException.class,
                () -> docNumberService.nextNumber("NO_SUCH_BIZ_KEY_2026"));
    }
}
