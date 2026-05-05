package com.linan.office.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeVehicleRequestService;
import com.linan.system.service.ApprovalFlowEngineService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用车申请 Controller 单元测试
 *
 * <p>覆盖本次审查修复的关键业务规则：禁止自审自批、审批人姓名使用真实用户名、取消限制。</p>
 */
@ExtendWith(MockitoExtension.class)
class OfficeVehicleRequestControllerTest {

    @Mock
    private IOfficeVehicleRequestService vehicleService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private com.linan.system.service.IApprovalFlowLogService flowLogService;

    @Mock
    private ApprovalFlowEngineService flowEngine;

    @InjectMocks
    private OfficeVehicleRequestController controller;

    private Long approverId;
    private Long applicantId;

    /**
     * 初始化 MyBatis-Plus 实体表信息缓存，使 LambdaUpdateWrapper 在纯 Mockito 环境下可构建
     * （正常 Spring 启动时由 mybatis-plus 自动注册，单元测试无 Spring 上下文需手动初始化）
     */
    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OfficeVehicleRequest.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        approverId = 1L;
        applicantId = 2L;
        setThreadLocal("currentUserIdHolder", approverId);
        setThreadLocal("currentUsernameHolder", "admin");
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("审批通过：审批人姓名为当前用户名")
    void approve_pass_usesRealUsername() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(20L);
        request.setApplicantId(applicantId);
        request.setStatus("0");
        when(vehicleService.getById(20L)).thenReturn(request);
        // 引擎：末节点通过 = 终态，业务侧负责更新业务表
        when(flowEngine.approve("vehicle", 20L, approverId, "admin", "pass", "同意出车"))
                .thenReturn(new ApprovalFlowEngineService.ApprovalResult(true, true, 1, "车辆管理员审批"));
        when(vehicleService.update(any())).thenReturn(true);

        Result<Void> result = controller.approve(20L, "pass", "同意出车");

        assertEquals(200, result.getCode());

        // 原子条件更新：仅当 status='0' 时生效（WHERE 含状态条件，防并发/重复审批覆盖结果）
        LambdaUpdateWrapper<OfficeVehicleRequest> wrapper = captureUpdateWrapper();
        assertTrue(wrapper.getSqlSegment().contains("status"));
        // SET 字段应包含状态、审批人ID/姓名、审批时间、审批意见
        String sqlSet = wrapper.getSqlSet();
        assertTrue(sqlSet.contains("status"));
        assertTrue(sqlSet.contains("approver_id"));
        assertTrue(sqlSet.contains("approver_name"));
        assertTrue(sqlSet.contains("approve_time"));
        assertTrue(sqlSet.contains("approve_remark"));
        // 参数值：目标状态、审批人ID、真实用户名、审批意见、审批时间
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        assertTrue(params.containsValue("1"));            // 已通过
        assertTrue(params.containsValue(approverId));      // 审批人用户ID
        assertTrue(params.containsValue("admin"));        // 真实用户名而非 userId 数字
        assertTrue(params.containsValue("同意出车"));      // 审批意见
        assertTrue(params.values().stream().anyMatch(v -> v instanceof String
                && ((String) v).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))); // 审批时间
    }

    @Test
    @DisplayName("禁止审批自己的用车申请")
    void approve_ownApplication_rejected() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(21L);
        request.setApplicantId(approverId);
        request.setStatus("0");
        when(vehicleService.getById(21L)).thenReturn(request);

        Result<Void> result = controller.approve(21L, "pass", null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不能审批自己的申请"));
        verify(vehicleService, never()).update(any());
    }

    @Test
    @DisplayName("取消：仅本人可取消自己的申请")
    void cancel_otherUsersApplication_rejected() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(22L);
        request.setApplicantId(99L);
        request.setStatus("0");
        when(vehicleService.getById(22L)).thenReturn(request);

        Result<Void> result = controller.cancel(22L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("只能取消自己的申请"));
        verify(vehicleService, never()).update(any());
    }

    @Test
    @DisplayName("已审批的申请不能重复审批（引擎无待审节点抛业务异常）")
    void approve_alreadyApproved_rejected() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(23L);
        request.setApplicantId(applicantId);
        request.setStatus("1");
        when(vehicleService.getById(23L)).thenReturn(request);
        // 引擎侧：无待审节点（已处理过）抛业务异常，由全局异常处理器统一转 500
        when(flowEngine.approve("vehicle", 23L, approverId, "admin", "pass", null))
                .thenThrow(new BusinessException("无待审批节点，可能已处理"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.approve(23L, "pass", null));

        assertTrue(ex.getMessage().contains("无待审批节点"));
        // 不应写审批流程日志
        verify(flowLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("提交申请：状态为待审批，申请人自动填充")
    void submit_setsStatusAndApplicant() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setStartTime("2026-08-11 09:00");
        request.setEndTime("2026-08-11 18:00");
        request.setPurpose("拜访客户");
        request.setApplicantName("李四");
        when(vehicleService.save(any(OfficeVehicleRequest.class))).thenReturn(true);

        Result<Void> result = controller.submit(request);

        assertEquals(200, result.getCode());
        assertEquals(approverId, request.getApplicantId());
        assertEquals("0", request.getStatus());
    }

    /** 捕获控制器调用 update(Wrapper) 时传入的原子条件更新包装器 */
    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<OfficeVehicleRequest> captureUpdateWrapper() {
        ArgumentCaptor<Wrapper<OfficeVehicleRequest>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(vehicleService).update(captor.capture());
        return (LambdaUpdateWrapper<OfficeVehicleRequest>) captor.getValue();
    }
}
