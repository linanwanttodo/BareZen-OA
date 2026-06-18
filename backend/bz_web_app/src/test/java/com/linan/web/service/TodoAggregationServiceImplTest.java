package com.linan.web.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.core.common.api.PageResult;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.hr.service.IAttOvertimeRequestService;
import com.linan.hr.service.IAttTravelRequestService;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeVehicleRequestService;
import com.linan.system.entity.ApprovalFlowTask;
import com.linan.system.service.IApprovalFlowTaskService;
import com.linan.web.service.impl.TodoAggregationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TodoAggregationServiceImpl 统一待办聚合单测
 *
 * <p>验证：以 approval_flow_task 待审节点数据库分页为唯一数据源（mock page）、
 * 页内按 bizType 批量补明细（mock 业务 service 的 lambdaQuery 链）、
 * 未知业务类型忽略、业务记录缺失跳过、summary 五类计数汇总。</p>
 */
@ExtendWith(MockitoExtension.class)
class TodoAggregationServiceImplTest {

    @Mock
    private IAttLeaveRequestService leaveService;

    @Mock
    private IAttOvertimeRequestService overtimeService;

    @Mock
    private IAttTravelRequestService travelService;

    @Mock
    private IOfficeVehicleRequestService vehicleService;

    @Mock
    private IOfficeSupplyRequestService supplyRequestService;

    @Mock
    private IApprovalFlowTaskService taskService;

    @InjectMocks
    private TodoAggregationServiceImpl todoService;

    /** 构造一个引擎待审节点 */
    private ApprovalFlowTask task(Long id, String bizType, Long bizId) {
        ApprovalFlowTask t = new ApprovalFlowTask();
        t.setId(id);
        t.setBizType(bizType);
        t.setBizId(bizId);
        t.setStatus("0");
        t.setCreateTime(LocalDateTime.of(2026, 8, 5, 10, 0));
        return t;
    }

    /** stub 业务 service：lambdaQuery().in(...).list() 返回给定记录 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> void stubListQuery(IService<T> service, List<T> records) {
        BaseMapper<T> mapper = mock(BaseMapper.class);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(records);
        when(service.lambdaQuery()).thenReturn(new LambdaQueryChainWrapper<>(mapper));
    }

    @Test
    @DisplayName("page：引擎待审节点数据库分页 + 页内批量补明细组装")
    void page_assemblesByBizType() {
        Page<ApprovalFlowTask> taskPage = new Page<>(1, 10);
        taskPage.setTotal(3);
        taskPage.setRecords(List.of(
                task(1L, "leave", 101L),
                task(2L, "vehicle", 202L),
                task(3L, "supply", 303L)));
        when(taskService.page(any(IPage.class), any(Wrapper.class))).thenReturn(taskPage);

        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(101L);
        leave.setUserName("张三");
        leave.setStartTime("2026-08-06 09:00");
        leave.setEndTime("2026-08-06 18:00");
        leave.setCreateTime(LocalDateTime.of(2026, 8, 5, 9, 0));
        stubListQuery(leaveService, List.of(leave));

        OfficeVehicleRequest vehicle = new OfficeVehicleRequest();
        vehicle.setId(202L);
        vehicle.setApplicantName("李四");
        vehicle.setStartTime("2026-08-07 08:00");
        vehicle.setEndTime("2026-08-07 20:00");
        vehicle.setCreateTime(LocalDateTime.of(2026, 8, 5, 10, 30));
        stubListQuery(vehicleService, List.of(vehicle));

        OfficeSupplyRequest supply = new OfficeSupplyRequest();
        supply.setId(303L);
        supply.setUserName("王五");
        supply.setSupplyName("笔记本");
        supply.setQuantity(2);
        supply.setCreateTime(LocalDateTime.of(2026, 8, 5, 11, 0));
        stubListQuery(supplyRequestService, List.of(supply));

        PageResult<TodoAggregationService.TodoItem> result = todoService.page(1L, 1, 10, null, null);

        // 数据库分页元数据透传
        assertEquals(3, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getPages());
        assertEquals(3, result.getList().size());

        // 请假（同构类型）
        TodoAggregationService.TodoItem item0 = result.getList().get(0);
        assertEquals("leave", item0.type());
        assertEquals("请假申请", item0.typeName());
        assertEquals("101", item0.id());
        assertEquals("张三 的请假申请", item0.title());
        assertEquals("张三", item0.applicantName());
        assertEquals("2026-08-06 09:00", item0.startTime());
        assertEquals("2026-08-06 18:00", item0.endTime());

        // 用车
        TodoAggregationService.TodoItem item1 = result.getList().get(1);
        assertEquals("vehicle", item1.type());
        assertEquals("李四 的用车申请", item1.title());
        assertEquals("李四", item1.applicantName());

        // 用品申领（非同构：标题含用品名与数量，无起止时间）
        TodoAggregationService.TodoItem item2 = result.getList().get(2);
        assertEquals("supply", item2.type());
        assertEquals("用品申领", item2.typeName());
        assertEquals("303", item2.id());
        assertEquals("王五 申领 笔记本 ×2", item2.title());
        assertNull(item2.startTime());
        assertNull(item2.endTime());

        verify(taskService).page(any(IPage.class), any(Wrapper.class));
    }

    @Test
    @DisplayName("page：未接入引擎的业务类型被忽略，不触达任何业务表")
    void page_unknownBizType_ignored() {
        Page<ApprovalFlowTask> taskPage = new Page<>(1, 10);
        taskPage.setTotal(1);
        taskPage.setRecords(List.of(task(1L, "salary", 501L)));
        when(taskService.page(any(IPage.class), any(Wrapper.class))).thenReturn(taskPage);

        PageResult<TodoAggregationService.TodoItem> result = todoService.page(1L, 1, 10, null, null);

        assertTrue(result.getList().isEmpty());
        verify(leaveService, never()).lambdaQuery();
        verify(vehicleService, never()).lambdaQuery();
        verify(supplyRequestService, never()).lambdaQuery();
    }

    @Test
    @DisplayName("page：业务记录已删除/缺失时跳过该待办")
    void page_missingBizRecord_skipped() {
        Page<ApprovalFlowTask> taskPage = new Page<>(1, 10);
        taskPage.setTotal(1);
        taskPage.setRecords(List.of(task(1L, "leave", 9999L)));
        when(taskService.page(any(IPage.class), any(Wrapper.class))).thenReturn(taskPage);
        stubListQuery(leaveService, List.of()); // 查不到业务记录

        PageResult<TodoAggregationService.TodoItem> result = todoService.page(1L, 1, 10, null, null);

        assertTrue(result.getList().isEmpty());
    }

    @Test
    @DisplayName("summary：五类待审节点计数汇总")
    void summary_countsByType() {
        BaseMapper<ApprovalFlowTask> taskMapper = mock(BaseMapper.class);
        when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(1L, 2L, 3L, 4L, 5L);
        when(taskService.lambdaQuery()).thenReturn(new LambdaQueryChainWrapper<>(taskMapper));

        Map<String, Long> summary = todoService.summary(1L);

        assertEquals(6, summary.size());
        assertEquals(1L, summary.get("leave"));
        assertEquals(2L, summary.get("overtime"));
        assertEquals(3L, summary.get("travel"));
        assertEquals(4L, summary.get("vehicle"));
        assertEquals(5L, summary.get("supply"));
        assertEquals(15L, summary.get("total"));
    }

    @Test
    @DisplayName("summary：无待办时 total=0")
    void summary_allZero() {
        BaseMapper<ApprovalFlowTask> taskMapper = mock(BaseMapper.class);
        when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(0L, 0L, 0L, 0L, 0L);
        when(taskService.lambdaQuery()).thenReturn(new LambdaQueryChainWrapper<>(taskMapper));

        Map<String, Long> summary = todoService.summary(1L);

        assertEquals(0L, summary.get("total"));
    }

    /** stub 业务 service 的 lambdaQuery 链为自返回 mock，便于 verify 链上被调用的约束 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> LambdaQueryChainWrapper<T> stubChainQuery(IService<T> service, List<T> records) {
        LambdaQueryChainWrapper<T> chain = mock(LambdaQueryChainWrapper.class);
        when(service.lambdaQuery()).thenReturn(chain);
        when(chain.like(any(), any())).thenReturn(chain);
        when(chain.list()).thenReturn(records);
        lenient().when(chain.last(anyString())).thenReturn(chain);
        return chain;
    }

    @Test
    @DisplayName("page：关键字命中的五类查询均带 LIMIT 1000，防无界扫描")
    void page_keywordHits_allCappedByLimit() {
        List<LambdaQueryChainWrapper<?>> chains = List.of(
                stubChainQuery(leaveService, List.of()),
                stubChainQuery(overtimeService, List.of()),
                stubChainQuery(travelService, List.of()),
                stubChainQuery(vehicleService, List.of()),
                stubChainQuery(supplyRequestService, List.of()));

        PageResult<TodoAggregationService.TodoItem> result = todoService.page(1L, 1, 10, null, "张");

        assertEquals(0, result.getTotal());
        for (LambdaQueryChainWrapper<?> chain : chains) {
            verify(chain).last("LIMIT 1000");
        }
        // 五表皆无命中时在引擎分页前提前返回
        verify(taskService, never()).page(any(IPage.class), any(Wrapper.class));
    }
}