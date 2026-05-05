package com.linan.web.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.hr.service.IAttOvertimeRequestService;
import com.linan.hr.service.IAttTravelRequestService;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeVehicleRequestService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * TodoCenterController 统一待办参数钳制与 LIMIT 单测
 *
 * <p>覆盖 T12：pageNum<1 / pageSize<1 / pageSize>100 直接返回参数错误（拒绝 subList 负索引与无界内存分页）；
 * 合法参数下两个聚合查询均追加 LIMIT 1000 防无界全表扫描。</p>
 */
@ExtendWith(MockitoExtension.class)
class TodoCenterControllerTest {

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

    private TodoCenterController controller;

    /**
     * 初始化实体表信息缓存：lambdaQuery() 构造 LambdaQueryChainWrapper 时需解析列缓存
     */
    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttLeaveRequest.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttOvertimeRequest.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttTravelRequest.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OfficeVehicleRequest.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OfficeSupplyRequest.class);
    }

    @BeforeEach
    void setUp() {
        controller = new TodoCenterController(leaveService, overtimeService, travelService, vehicleService, supplyRequestService);
    }

    @Test
    @DisplayName("pageNum=0 直接返回参数错误，不触达服务层")
    void list_pageNumZero_rejected() {
        Result<PageResult<TodoCenterController.TodoItem>> result = controller.list(0, 10);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("分页参数不合法"));
        verifyNoInteractions(leaveService, overtimeService, travelService, vehicleService, supplyRequestService);
    }

    @Test
    @DisplayName("pageSize=1000 越界直接返回参数错误，不触达服务层")
    void list_pageSizeTooLarge_rejected() {
        Result<PageResult<TodoCenterController.TodoItem>> result = controller.list(1, 1000);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("分页参数不合法"));
        verifyNoInteractions(leaveService, overtimeService, travelService, vehicleService, supplyRequestService);
    }

    @Test
    @DisplayName("pageSize=0 直接返回参数错误")
    void list_pageSizeZero_rejected() {
        Result<PageResult<TodoCenterController.TodoItem>> result = controller.list(1, 0);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("分页参数不合法"));
        verifyNoInteractions(leaveService, overtimeService, travelService, vehicleService, supplyRequestService);
    }

    @Test
    @DisplayName("合法参数：五个聚合查询均携带 LIMIT 1000")
    void list_validParams_queriesLimited() {
        // lambdaQuery() 是 IService 默认方法，Mockito 会拦截返回 null，
        // 因此用真实 LambdaQueryChainWrapper（底层挂 mock BaseMapper）替换
        BaseMapper<AttLeaveRequest> leaveMapper = mock(BaseMapper.class);
        BaseMapper<AttOvertimeRequest> overtimeMapper = mock(BaseMapper.class);
        BaseMapper<AttTravelRequest> travelMapper = mock(BaseMapper.class);
        BaseMapper<OfficeVehicleRequest> vehicleMapper = mock(BaseMapper.class);
        BaseMapper<OfficeSupplyRequest> supplyMapper = mock(BaseMapper.class);
        when(leaveMapper.selectList(any())).thenReturn(List.of());
        when(overtimeMapper.selectList(any())).thenReturn(List.of());
        when(travelMapper.selectList(any())).thenReturn(List.of());
        when(vehicleMapper.selectList(any())).thenReturn(List.of());
        when(supplyMapper.selectList(any())).thenReturn(List.of());
        when(leaveService.lambdaQuery())
                .thenReturn(new LambdaQueryChainWrapper<>(leaveMapper));
        when(overtimeService.lambdaQuery())
                .thenReturn(new LambdaQueryChainWrapper<>(overtimeMapper));
        when(travelService.lambdaQuery())
                .thenReturn(new LambdaQueryChainWrapper<>(travelMapper));
        when(vehicleService.lambdaQuery())
                .thenReturn(new LambdaQueryChainWrapper<>(vehicleMapper));
        when(supplyRequestService.lambdaQuery())
                .thenReturn(new LambdaQueryChainWrapper<>(supplyMapper));

        Result<PageResult<TodoCenterController.TodoItem>> result = controller.list(1, 10);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().getTotal());

        // 五个待办查询都必须携带 LIMIT 1000
        assertCapturedHasLimit(leaveMapper);
        assertCapturedHasLimit(overtimeMapper);
        assertCapturedHasLimit(travelMapper);
        assertCapturedHasLimit(vehicleMapper);
        assertCapturedHasLimit(supplyMapper);
    }

    /** 断言 mock mapper 捕获到的查询 wrapper 含 LIMIT 1000 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void assertCapturedHasLimit(BaseMapper mapper) {
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().contains("LIMIT 1000"),
                "待办查询必须加 LIMIT 1000");
    }
}