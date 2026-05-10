package com.linan.core.common.test;

import com.linan.asset.entity.AssetCategory;
import com.linan.asset.entity.AssetBorrowRecord;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.system.entity.SysUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 测试数据构建器
 *
 * 提供构建测试实体对象的工具方法，简化测试代码
 *
 * @author BareZen Team
 */
public class TestDataBuilder {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 构建测试用的系统用户
     */
    public static SysUser createTestUser(Long id, String username) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUserName(username);
        user.setNickName(username);
        user.setPassword("$2a$10$testPasswordHash");
        user.setEmail(username + "@example.com");
        user.setPhone("13800138000");
        user.setSex("0");
        user.setStatus("0");
        return user;
    }

    /**
     * 构建测试用的资产分类
     */
    public static AssetCategory createTestCategory(Long id, String name) {
        AssetCategory category = new AssetCategory();
        category.setId(id);
        category.setName(name);
        category.setParentId(0L);
        category.setSort(1);
        category.setStatus(1);
        return category;
    }

    /**
     * 构建测试用的资产借用记录
     */
    public static AssetBorrowRecord createTestBorrowRecord(Long id, Long assetId, Long borrowerId) {
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setId(id);
        record.setAssetId(assetId);
        record.setBorrowerId(borrowerId);
        record.setBorrowerName("测试用户");
        record.setBorrowTime(LocalDateTime.now().format(FMT));
        record.setExpectReturnTime(LocalDateTime.now().plusDays(7).format(FMT));
        record.setRemark("测试借用");
        record.setStatus("0"); // 借用中
        return record;
    }

    /**
     * 构建测试用的请假申请
     */
    public static AttLeaveRequest createTestLeaveRequest(Long id, Long userId) {
        AttLeaveRequest request = new AttLeaveRequest();
        request.setId(id);
        request.setUserId(userId);
        request.setUserName("测试用户");
        request.setStartTime(LocalDateTime.now().plusDays(1).format(FMT));
        request.setEndTime(LocalDateTime.now().plusDays(3).format(FMT));
        request.setLeaveType("1"); // 年假
        request.setLeaveDays(new BigDecimal("2"));
        request.setReason("测试请假");
        request.setStatus("0"); // 待审批
        return request;
    }

    /**
     * 创建随机的测试数据字符串
     */
    public static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    /**
     * 创建随机的测试邮箱
     */
    public static String randomEmail() {
        return "test" + randomString(8) + "@example.com";
    }

    /**
     * 创建随机的测试手机号
     */
    public static String randomPhone() {
        return "138" + String.format("%08d", (int) (Math.random() * 100000000));
    }
}