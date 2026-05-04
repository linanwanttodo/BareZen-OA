package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.service.IAttLeaveRequestService;
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
 * 请假审批 Controller 单元测试
 *
 * <p>覆盖本次审查修复的关键业务规则：禁止自审自批、审批人姓名使用真实用户名。</p>
 */
@ExtendWith(MockitoExtension.class)
class AttLeaveRequestControllerTest {

    @Mock
    private IAttLeaveRequestService leaveService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private com.linan.system.service.IApprovalFlowLogService flowLogService;

    @InjectMocks
    private AttLeaveRequestController controller;

    private Long approverId;
    private Long applicantId;

    /**
     * 初始化 MyBatis-Plus 实体表信息缓存，使 LambdaUpdateWrapper 在纯 Mockito 环境下可构建
     * （正常 Spring 启动时由 mybatis-plus 自动注册，单元测试无 Spring 上下文需手动初始化）
     */
    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttLeaveRequest.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        approverId = 1L;
        applicantId = 2L;

        // 通过反射设置 ThreadLocal 模拟当前登录用户（审批人）
        setThreadLocal("currentUserIdHolder", approverId);
        setThreadLocal("currentUsernameHolder", "admin");
    }

    @AfterEach
    void tearDown() throws Exception {
        AuthInterceptor.clearCurrentUser();
    }

    /** 反射设置 AuthInterceptor 的私有 ThreadLocal */
    private void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("审批通过：状态置为已通过，审批人姓名为当前用户名")
    void approve_pass_usesRealUsername() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(10L);
        leave.setUserId(applicantId); // 申请人不是当前审批人
        leave.setStatus("0");
        when(leaveService.getById(10L)).thenReturn(leave);
        when(leaveService.update(any())).thenReturn(true);

        Result<Void> result = controller.approve(10L, "pass", "同意");

        assertNotNull(result);
        assertEquals(200, result.getCode());

        // 原子条件更新：仅当 status='0' 时生效（WHERE 含状态条件，防并发/重复审批覆盖结果）
        LambdaUpdateWrapper<AttLeaveRequest> wrapper = captureUpdateWrapper();
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
        assertTrue(params.containsValue("同意"));          // 审批意见
        assertTrue(params.values().stream().anyMatch(v -> v instanceof String
                && ((String) v).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))); // 审批时间
    }

    @Test
    @DisplayName("禁止审批自己的申请")
    void approve_ownApplication_rejected() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(11L);
        leave.setUserId(approverId); // 申请人与审批人相同
        leave.setStatus("0");
        when(leaveService.getById(11L)).thenReturn(leave);

        Result<Void> result = controller.approve(11L, "pass", null);

        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不能审批自己的申请"));
        // 不允许更新
        verify(leaveService, never()).update(any());
    }

    @Test
    @DisplayName("重复审批被拒绝")
    void approve_alreadyApproved_rejected() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(12L);
        leave.setUserId(applicantId);
        leave.setStatus("1"); // 已审批
        when(leaveService.getById(12L)).thenReturn(leave);
        // 模拟原子条件更新受影响行数为 0（WHERE status='0' 不匹配，返回 false）
        when(leaveService.update(any())).thenReturn(false);

        Result<Void> result = controller.approve(12L, "pass", null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("已审批"));
        // 不应写审批流程日志
        verify(flowLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("驳回：状态置为已驳回")
    void approve_reject_setsStatus() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(13L);
        leave.setUserId(applicantId);
        leave.setStatus("0");
        when(leaveService.getById(13L)).thenReturn(leave);
        when(leaveService.update(any())).thenReturn(true);

        Result<Void> result = controller.approve(13L, "reject", "事由不充分");

        assertEquals(200, result.getCode());
        Map<String, Object> params = captureUpdateWrapper().getParamNameValuePairs();
        assertTrue(params.containsValue("2"));           // 已驳回
        assertTrue(params.containsValue("事由不充分"));    // 审批意见
    }

    @Test
    @DisplayName("无效审批动作被拒绝")
    void approve_invalidAction_rejected() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(14L);
        leave.setUserId(applicantId);
        leave.setStatus("0");
        when(leaveService.getById(14L)).thenReturn(leave);

        Result<Void> result = controller.approve(14L, "unknown", null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("无效的审批动作"));
        verify(leaveService, never()).update(any());
    }

    @Test
    @DisplayName("撤销：仅本人可撤销自己的申请")
    void revoke_otherUsersApplication_rejected() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(15L);
        leave.setUserId(99L); // 不是当前用户
        leave.setStatus("0");
        when(leaveService.getById(15L)).thenReturn(leave);

        Result<Void> result = controller.revoke(15L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("只能撤销自己的申请"));
        verify(leaveService, never()).update(any());
    }

    @Test
    @DisplayName("提交申请：状态为待审批，申请人自动填充")
    void submit_setsStatusAndApplicant() {
        AttLeaveRequest request = new AttLeaveRequest();
        request.setStartTime("2026-08-10 09:00");
        request.setEndTime("2026-08-10 18:00");
        request.setUserName("张三");
        when(leaveService.save(any(AttLeaveRequest.class))).thenReturn(true);

        Result<Void> result = controller.submit(request);

        assertEquals(200, result.getCode());
        assertEquals(approverId, request.getUserId());
        assertEquals("0", request.getStatus());
    }

    /** 捕获控制器调用 update(Wrapper) 时传入的原子条件更新包装器 */
    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<AttLeaveRequest> captureUpdateWrapper() {
        ArgumentCaptor<Wrapper<AttLeaveRequest>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(leaveService).update(captor.capture());
        return (LambdaUpdateWrapper<AttLeaveRequest>) captor.getValue();
    }
}
