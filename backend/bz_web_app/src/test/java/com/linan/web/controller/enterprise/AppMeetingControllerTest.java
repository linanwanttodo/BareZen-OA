package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeMeetingBooking;
import com.linan.office.entity.OfficeMeetingRoom;
import com.linan.office.service.IOfficeMeetingBookingService;
import com.linan.office.service.IOfficeMeetingRoomService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppMeetingController 提交/详情/取消/时段可用查询接口单元测试
 *
 * <p>对齐 AppVehicleControllerTest：身份字段服务端强制覆盖、冗余字段清空、
 * 冲突异常传播触发事务回滚；取消带归属条件的原子删除；详情/取消归属校验。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppMeetingControllerTest {

    @Mock
    private IOfficeMeetingBookingService bookingService;

    @Mock
    private IOfficeMeetingRoomService roomService;

    private AppMeetingController controller;

    /**
     * 初始化 MyBatis-Plus 实体表信息缓存，使控制器内构建的 LambdaQueryWrapper 在纯 Mockito 环境下可用
     */
    @BeforeAll
    static void initTableInfoCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, OfficeMeetingBooking.class);
        TableInfoHelper.initTableInfo(assistant, OfficeMeetingRoom.class);
    }

    @BeforeEach
    void setUp() {
        controller = new AppMeetingController(bookingService, roomService);
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
    @DisplayName("提交预订：伪造 bookerId/bookerName/roomName/id 被服务端强制覆盖或清空")
    void submit_forgedIdentity_overridden() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        setThreadLocal("currentUsernameHolder", "meetingUser");

        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setRoomId(3L);
        booking.setTitle("项目周会");
        booking.setStartTime("2026-08-11 09:00");
        booking.setEndTime("2026-08-11 10:00");
        booking.setBookerId(999L);        // 伪造
        booking.setBookerName("伪造者");   // 伪造
        booking.setRoomName("伪造会议室"); // 伪造冗余字段
        booking.setId(54321L);            // 预置主键
        var result = controller.submit(booking);

        assertEquals(200, result.getCode());
        assertEquals(9L, booking.getBookerId(), "bookerId 必须为服务端当前登录用户");
        assertEquals("meetingUser", booking.getBookerName(), "bookerName 必须为服务端真实用户名");
        assertNull(booking.getRoomName(), "roomName 必须被清空（由 saveBooking 从锁定行回填）");
        assertNull(booking.getId(), "id 必须被清空（防主键冲突/覆盖）");
        verify(bookingService).saveBooking(booking);
    }

    @Test
    @DisplayName("提交预订：缺会议室或时间缺失/倒序返回错误，不落库")
    void submit_invalidParams_rejected() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);

        OfficeMeetingBooking noRoom = new OfficeMeetingBooking();
        noRoom.setTitle("项目周会");
        noRoom.setStartTime("2026-08-11 09:00");
        noRoom.setEndTime("2026-08-11 10:00");
        var noRoomResult = controller.submit(noRoom);
        assertEquals(500, noRoomResult.getCode());
        assertEquals("请选择会议室", noRoomResult.getMsg());

        OfficeMeetingBooking missing = new OfficeMeetingBooking();
        missing.setRoomId(3L);
        missing.setTitle("项目周会");
        missing.setStartTime("2026-08-11 09:00");
        var missingResult = controller.submit(missing);
        assertEquals(500, missingResult.getCode());
        assertEquals("请选择会议时间", missingResult.getMsg());

        OfficeMeetingBooking reversed = new OfficeMeetingBooking();
        reversed.setRoomId(3L);
        reversed.setTitle("项目周会");
        reversed.setStartTime("2026-08-11 10:00");
        reversed.setEndTime("2026-08-11 09:00");
        var reversedResult = controller.submit(reversed);
        assertEquals(500, reversedResult.getCode());
        assertEquals("结束时间必须晚于开始时间", reversedResult.getMsg());

        verify(bookingService, never()).saveBooking(any(OfficeMeetingBooking.class));
    }

    @Test
    @DisplayName("提交预订：时段冲突时 saveBooking 抛 BusinessException 向上传播（触发事务回滚）")
    void submit_roomConflict_propagatesException() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        setThreadLocal("currentUsernameHolder", "meetingUser");
        org.mockito.Mockito.doThrow(new BusinessException("该会议室在所选时间段已被预订"))
                .when(bookingService).saveBooking(any(OfficeMeetingBooking.class));

        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setRoomId(3L);
        booking.setTitle("项目周会");
        booking.setStartTime("2026-08-11 09:00");
        booking.setEndTime("2026-08-11 10:00");

        assertThrows(BusinessException.class, () -> controller.submit(booking),
                "冲突必须向上抛异常，全局异常处理器统一返回、事务回滚已保存数据");
    }

    @Test
    @DisplayName("提交预订方法声明 @Transactional（事务一致性）")
    void submit_hasTransactionalAnnotation() throws Exception {
        assertNotNull(AppMeetingController.class
                .getMethod("submit", OfficeMeetingBooking.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    @Test
    @DisplayName("详情：本人可查；非本人被拒绝，不存在返回错误")
    void detail_ownershipEnforced() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);

        OfficeMeetingBooking mine = new OfficeMeetingBooking();
        mine.setId(10L);
        mine.setBookerId(1L);
        mine.setTitle("项目周会");
        when(bookingService.getById(10L)).thenReturn(mine);

        var ownResult = controller.detail(10L);
        assertEquals(200, ownResult.getCode());
        assertEquals("项目周会", ownResult.getData().getTitle());

        OfficeMeetingBooking others = new OfficeMeetingBooking();
        others.setId(11L);
        others.setBookerId(2L);
        when(bookingService.getById(11L)).thenReturn(others);
        assertEquals(500, controller.detail(11L).getCode(), "非本人预订不可查看");

        when(bookingService.getById(12L)).thenReturn(null);
        assertEquals(500, controller.detail(12L).getCode(), "记录不存在返回错误");
    }

    @Test
    @DisplayName("取消成功：本人预订执行带归属条件的原子删除")
    void cancel_success_conditionalRemove() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setId(1L);
        booking.setBookerId(1L);
        booking.setTitle("项目周会");
        when(bookingService.getById(1L)).thenReturn(booking);
        when(bookingService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);

        var result = controller.cancel(1L);

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(bookingService).remove(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("booker_id"), "删除条件必须包含归属字段 booker_id");
        assertTrue(sqlSegment.contains("id"), "删除条件必须包含主键 id");
        verify(bookingService, never()).removeById(anyLong());
    }

    @Test
    @DisplayName("取消：非本人预订被拒绝，不触达删除")
    void cancel_notOwner_rejected() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        OfficeMeetingBooking others = new OfficeMeetingBooking();
        others.setId(11L);
        others.setBookerId(2L);
        when(bookingService.getById(11L)).thenReturn(others);

        var result = controller.cancel(11L);

        assertEquals(500, result.getCode());
        assertEquals("无权取消该预订", result.getMsg());
        verify(bookingService, never()).remove(any(LambdaQueryWrapper.class));
        verify(bookingService, never()).removeById(anyLong());
    }

    @Test
    @DisplayName("取消：记录不存在返回错误")
    void cancel_notExist_rejected() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        when(bookingService.getById(99L)).thenReturn(null);

        var result = controller.cancel(99L);

        assertEquals(500, result.getCode());
        assertEquals("记录不存在", result.getMsg());
        verify(bookingService, never()).remove(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("取消接口声明 @OperLog 审计")
    void cancel_hasOperLogAnnotation() throws Exception {
        var method = AppMeetingController.class.getMethod("cancel", Long.class);
        assertNotNull(method.getAnnotation(com.linan.core.common.annotation.OperLog.class));
    }

    @Test
    @DisplayName("时段可用会议室：排除已被预订的会议室，仅返回启用状态空闲房间")
    void availableRooms_excludesBusyRooms() {
        OfficeMeetingRoom free = new OfficeMeetingRoom();
        free.setId(1L);
        free.setRoomName("A会议室");
        free.setStatus("0");
        OfficeMeetingRoom busy = new OfficeMeetingRoom();
        busy.setId(2L);
        busy.setRoomName("B会议室");
        busy.setStatus("0");
        when(roomService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(free, busy));

        OfficeMeetingBooking occupied = new OfficeMeetingBooking();
        occupied.setRoomId(2L);
        occupied.setStartTime("2026-08-11 09:00");
        occupied.setEndTime("2026-08-11 10:00");
        when(bookingService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(occupied));

        var result = controller.availableRooms("2026-08-11 09:30", "2026-08-11 11:00");

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size(), "被占用的会议室必须被排除");
        assertEquals("A会议室", result.getData().get(0).getRoomName());
    }

    @Test
    @DisplayName("时段可用会议室：时段缺失或倒序返回错误，不触达查询")
    void availableRooms_invalidRange_rejected() {
        var missing = controller.availableRooms("", "2026-08-11 11:00");
        assertEquals(500, missing.getCode());
        assertEquals("请选择查询时段", missing.getMsg());

        var reversed = controller.availableRooms("2026-08-11 11:00", "2026-08-11 09:00");
        assertEquals(500, reversed.getCode());
        assertEquals("结束时间必须晚于开始时间", reversed.getMsg());

        verify(roomService, never()).list(any(LambdaQueryWrapper.class));
        verify(bookingService, never()).list(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("我的预订列表：未登录返回错误")
    void myBookings_notLogin_rejected() {
        var result = controller.myBookings(1, 10);

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
    }
}
