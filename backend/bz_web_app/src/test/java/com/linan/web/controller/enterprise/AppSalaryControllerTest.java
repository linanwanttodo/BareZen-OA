package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.plugin.salary.entity.SalaryPaymentRecord;
import com.linan.plugin.salary.entity.SalaryStructure;
import com.linan.plugin.salary.service.ISalaryPaymentRecordService;
import com.linan.plugin.salary.service.ISalaryStructureService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppSalaryController 查询接口单元测试
 *
 * <p>工资数据敏感：验证所有查询强制使用当前登录用户身份（不接受前端 userId）、
 * 详情归属校验拒绝越权、分页参数钳制。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppSalaryControllerTest {

    @Mock
    private ISalaryPaymentRecordService payrollService;

    @Mock
    private ISalaryStructureService structureService;

    private AppSalaryController controller;

    @BeforeAll
    static void initTableInfoCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SalaryPaymentRecord.class);
        TableInfoHelper.initTableInfo(assistant, SalaryStructure.class);
    }

    @BeforeEach
    void setUp() {
        controller = new AppSalaryController(payrollService, structureService);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    /** 反射注入当前登录用户（AuthInterceptor 的 ThreadLocal 无公开 setter） */
    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("工资条列表：强制按当前登录用户过滤，按发放月份倒序")
    void myPayrolls_forcedCurrentUserAndOrder() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        Page<SalaryPaymentRecord> pageResult = new Page<>(1, 10);
        SalaryPaymentRecord record = new SalaryPaymentRecord();
        record.setId(1L);
        record.setUserId(9L);
        pageResult.setRecords(java.util.List.of(record));
        pageResult.setTotal(1);
        when(payrollService.page(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        var result = controller.myPayrolls(1, 10);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getList().size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(payrollService).page(any(IPage.class), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("user_id"), "查询条件必须包含归属字段 user_id");
        assertTrue(sqlSegment.contains("payment_month") && sqlSegment.contains("DESC"),
                "必须按发放月份倒序");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(9L),
                "userId 必须取当前登录用户");
    }

    @Test
    @DisplayName("工资条列表：接口不接收 userId，无法伪造他人身份")
    void myPayrolls_noUserIdParameter() throws Exception {
        Method listMethod = AppSalaryController.class.getMethod("myPayrolls", Integer.class, Integer.class);
        assertEquals(2, listMethod.getParameterCount(), "列表接口只允许分页参数");
        assertTrue(Arrays.stream(listMethod.getParameters()).map(Parameter::getName).noneMatch("userId"::equals));
        Method detailMethod = AppSalaryController.class.getMethod("payrollDetail", Long.class);
        assertEquals(1, detailMethod.getParameterCount(), "详情接口只允许记录 id");
        assertEquals(0, AppSalaryController.class.getMethod("myStructure").getParameterCount(),
                "工资结构接口不接受任何参数");
    }

    @Test
    @DisplayName("工资条列表：未登录返回错误")
    void myPayrolls_notLogin_rejected() {
        Result<?> result = controller.myPayrolls(1, 10);

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
        verify(payrollService, never()).page(any(), any());
    }

    @Test
    @DisplayName("工资条列表：分页参数越界被钳制")
    void myPayrolls_invalidPage_rejected() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);

        assertThrows(BusinessException.class, () -> controller.myPayrolls(0, 10));
        assertThrows(BusinessException.class, () -> controller.myPayrolls(1, 101));
        assertThrows(BusinessException.class, () -> controller.myPayrolls(1, -1));
        verify(payrollService, never()).page(any(), any());
    }

    @Test
    @DisplayName("工资条详情：本人可查，非本人被拒，记录不存在返回错误")
    void payrollDetail_ownershipEnforced() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);

        SalaryPaymentRecord mine = new SalaryPaymentRecord();
        mine.setId(10L);
        mine.setUserId(1L);
        mine.setActualSalary(new BigDecimal("8800.00"));
        when(payrollService.getById(10L)).thenReturn(mine);
        Result<SalaryPaymentRecord> ownResult = controller.payrollDetail(10L);
        assertEquals(200, ownResult.getCode());
        assertEquals(0, new BigDecimal("8800.00").compareTo(ownResult.getData().getActualSalary()));

        SalaryPaymentRecord others = new SalaryPaymentRecord();
        others.setId(11L);
        others.setUserId(2L);
        when(payrollService.getById(11L)).thenReturn(others);
        Result<SalaryPaymentRecord> othersResult = controller.payrollDetail(11L);
        assertEquals(500, othersResult.getCode());
        assertEquals("无权查看该工资条", othersResult.getMsg());

        Result<SalaryPaymentRecord> missingResult = controller.payrollDetail(12L);
        assertEquals(500, missingResult.getCode());
        assertEquals("工资条不存在", missingResult.getMsg());
    }

    @Test
    @DisplayName("工资条详情：未登录返回错误")
    void payrollDetail_notLogin_rejected() {
        Result<?> result = controller.payrollDetail(10L);

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
        verify(payrollService, never()).getById(any());
    }

    @Test
    @DisplayName("工资结构：复用按 userId 查询方法，强制当前登录用户")
    void myStructure_usesCurrentUserId() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        SalaryStructure structure = new SalaryStructure();
        structure.setId(1L);
        structure.setUserId(9L);
        when(structureService.getByUserId(9L)).thenReturn(structure);

        Result<SalaryStructure> result = controller.myStructure();

        assertEquals(200, result.getCode());
        assertEquals(9L, result.getData().getUserId());
        verify(structureService).getByUserId(9L);
        verify(structureService, never()).getByUserId(999L);
    }

    @Test
    @DisplayName("工资结构：未登录返回错误")
    void myStructure_notLogin_rejected() {
        Result<?> result = controller.myStructure();

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
        verify(structureService, never()).getByUserId(any());
    }
}
