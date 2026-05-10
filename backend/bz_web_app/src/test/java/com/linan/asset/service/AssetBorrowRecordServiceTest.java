package com.linan.asset.service;

import com.linan.asset.entity.AssetBorrowRecord;
import com.linan.core.common.exception.BusinessException;
import com.linan.web.BareZenApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资产借用记录Service集成测试
 *
 * 测试实际的数据库操作和业务逻辑（真实 MySQL barezen_test 库，Flyway 自动建表）
 *
 * @author BareZen Team
 */
@SpringBootTest(classes = BareZenApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("资产借用记录Service集成测试")
class AssetBorrowRecordServiceTest {

    @Autowired
    private IAssetBorrowRecordService borrowRecordService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("创建借用记录 - 正常情况")
    void createBorrowRecord_valid_success() {
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setAssetId(1L);
        record.setBorrowerId(100L);
        record.setBorrowerName("张三");
        record.setBorrowTime(LocalDateTime.now().format(FMT));
        record.setExpectReturnTime(LocalDateTime.now().plusDays(7).format(FMT));
        record.setRemark("项目使用");

        assertDoesNotThrow(() -> borrowRecordService.createBorrowRecord(record));
    }

    @Test
    @DisplayName("创建借用记录 - 资产ID为空抛出异常")
    void createBorrowRecord_nullAssetId_throwsException() {
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setAssetId(null);
        record.setBorrowerId(100L);
        record.setBorrowerName("张三");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowRecordService.createBorrowRecord(record));

        assertTrue(exception.getMessage().contains("资产ID不能为空"));
    }

    @Test
    @DisplayName("创建借用记录 - 借用人ID为空抛出异常")
    void createBorrowRecord_nullBorrowerId_throwsException() {
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setAssetId(1L);
        record.setBorrowerId(null);
        record.setBorrowerName("张三");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowRecordService.createBorrowRecord(record));

        assertTrue(exception.getMessage().contains("借用人ID不能为空"));
    }

    @Test
    @DisplayName("创建借用记录 - 借用人姓名为空抛出异常")
    void createBorrowRecord_nullBorrowerName_throwsException() {
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setAssetId(1L);
        record.setBorrowerId(100L);
        record.setBorrowerName("");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowRecordService.createBorrowRecord(record));

        assertTrue(exception.getMessage().contains("借用人姓名不能为空"));
    }

    @Test
    @DisplayName("检查资产可用性 - 没有借用记录返回true")
    void isAssetAvailable_noBorrowRecords_returnsTrue() {
        // 假设资产ID 999 没有借用记录
        boolean available = borrowRecordService.isAssetAvailable(999L);
        assertTrue(available);
    }

    @Test
    @DisplayName("归还资产 - 有效记录归还成功")
    void returnAsset_valid_success() {
        // 先创建一个借用记录
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setAssetId(999L);
        record.setBorrowerId(100L);
        record.setBorrowerName("测试用户");
        record.setBorrowTime(LocalDateTime.now().format(FMT));
        record.setExpectReturnTime(LocalDateTime.now().plusDays(7).format(FMT));
        
        borrowRecordService.createBorrowRecord(record);
        
        // 归还资产
        boolean result = borrowRecordService.returnAsset(record.getId());
        assertTrue(result);
    }

    @Test
    @DisplayName("归还资产 - 记录不存在抛出异常")
    void returnAsset_recordNotExists_throwsException() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowRecordService.returnAsset(99999L));

        assertTrue(exception.getMessage().contains("借用记录不存在"));
    }

    @Test
    @DisplayName("查询用户借用记录 - 有效用户返回列表")
    void listByBorrowerId_validUser_returnsList() {
        // 创建借用记录
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setAssetId(999L);
        record.setBorrowerId(888L);
        record.setBorrowerName("测试用户");
        record.setBorrowTime(LocalDateTime.now().format(FMT));
        record.setExpectReturnTime(LocalDateTime.now().plusDays(7).format(FMT));
        
        borrowRecordService.createBorrowRecord(record);
        
        // 查询该用户的借用记录
        var records = borrowRecordService.listByBorrowerId(888L);
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }

    @Test
    @DisplayName("查询用户借用记录 - 无效用户返回空列表")
    void listByBorrowerId_invalidUser_returnsEmpty() {
        var records = borrowRecordService.listByBorrowerId(99999L);
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }
}