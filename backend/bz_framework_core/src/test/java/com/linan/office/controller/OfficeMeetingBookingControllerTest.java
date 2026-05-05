package com.linan.office.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.PermissionService;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeMeetingBooking;
import com.linan.office.service.IOfficeMeetingBookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 会议预订 Controller 单元测试
 *
 * <p>业务规则（会议室存在性/时间冲突）已下沉至 Service，由
 * {@link com.linan.office.service.impl.OfficeMeetingBookingServiceImpl} 测试覆盖；
 * 此处聚焦 Controller 的参数校验与委托行为。</p>
 */
@ExtendWith(MockitoExtension.class)
class OfficeMeetingBookingControllerTest {

    @Mock
    private IOfficeMeetingBookingService bookingService;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private OfficeMeetingBookingController controller;

    @BeforeEach
    void setUp() throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField("currentUserIdHolder");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(1L);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    @Test
    @DisplayName("未选择会议室时拒绝预订")
    void save_roomIdMissing_rejected() {
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setTitle("周会");
        booking.setStartTime("2026-08-10 09:00");
        booking.setEndTime("2026-08-10 10:00");

        Result<Void> result = controller.save(booking);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("请选择会议室"));
        verify(bookingService, never()).saveBooking(any());
    }

    @Test
    @DisplayName("结束时间不晚于开始时间时拒绝")
    void save_invalidTimeRange_rejected() {
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setRoomId(1L);
        booking.setTitle("周会");
        booking.setStartTime("2026-08-10 10:00");
        booking.setEndTime("2026-08-10 09:00");

        Result<Void> result = controller.save(booking);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("结束时间必须晚于开始时间"));
        verify(bookingService, never()).saveBooking(any());
    }

    @Test
    @DisplayName("参数合法时委托 Service 预订")
    void save_validBooking_delegates() {
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setRoomId(1L);
        booking.setTitle("项目评审会");
        booking.setStartTime("2026-08-10 14:00");
        booking.setEndTime("2026-08-10 15:00");

        Result<Void> result = controller.save(booking);

        assertEquals(200, result.getCode());
        verify(bookingService, times(1)).saveBooking(booking);
    }

    @Test
    @DisplayName("Service 业务失败（如会议室不存在）时异常向上传播")
    void save_businessFailure_propagates() {
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setRoomId(999L);
        booking.setTitle("周会");
        booking.setStartTime("2026-08-10 09:00");
        booking.setEndTime("2026-08-10 10:00");

        doThrow(new BusinessException("会议室不存在"))
                .when(bookingService).saveBooking(any(OfficeMeetingBooking.class));

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.save(booking));
        assertTrue(ex.getMessage().contains("会议室不存在"));
    }

    // ==================== T13：删除所有权双轨 ====================

    @Test
    @DisplayName("预订人本人删除自己的预订放行")
    void delete_bookerOwner_allowed() {
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setId(1L);
        booking.setBookerId(1L);
        when(bookingService.getById(1L)).thenReturn(booking);
        when(bookingService.removeById(1L)).thenReturn(true);

        Result<Void> result = controller.delete(1L);

        assertEquals(200, result.getCode());
        verify(bookingService).removeById(1L);
    }

    @Test
    @DisplayName("非预订人且无权限：拒绝且不触库")
    void delete_notOwnerNoPermission_rejected() throws Exception {
        setCurrentUser(2L);
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setId(2L);
        booking.setBookerId(1L);
        when(bookingService.getById(2L)).thenReturn(booking);
        when(permissionService.hasPermission(2L, "office:meeting:delete")).thenReturn(false);

        Result<Void> result = controller.delete(2L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("无权限"));
        verify(bookingService, never()).removeById(2L);
    }

    @Test
    @DisplayName("非预订人但持删除权限（管理员旁路）：放行")
    void delete_notOwnerWithPermission_allowed() throws Exception {
        setCurrentUser(2L);
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setId(3L);
        booking.setBookerId(1L);
        when(bookingService.getById(3L)).thenReturn(booking);
        when(permissionService.hasPermission(2L, "office:meeting:delete")).thenReturn(true);
        when(bookingService.removeById(3L)).thenReturn(true);

        Result<Void> result = controller.delete(3L);

        assertEquals(200, result.getCode());
        verify(bookingService).removeById(3L);
    }

    @Test
    @DisplayName("方法级 @RequirePermission 已移除，@OperLog 保留")
    void delete_annotationContract() throws Exception {
        java.lang.reflect.Method method =
                OfficeMeetingBookingController.class.getMethod("delete", Long.class);
        assertNull(method.getAnnotation(RequirePermission.class),
                "delete 必须移除方法级 @RequirePermission，否则预订人本人被切面先行拦截");
        assertNotNull(method.getAnnotation(OperLog.class), "delete 必须保留 @OperLog");
    }

    /** 反射覆盖当前登录用户（已有测试默认 1L，此处用于非本人场景） */
    private void setCurrentUser(Long userId) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField("currentUserIdHolder");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(userId);
    }
}
