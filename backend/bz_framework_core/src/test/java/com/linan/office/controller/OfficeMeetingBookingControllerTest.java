package com.linan.office.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeMeetingBooking;
import com.linan.office.entity.OfficeMeetingRoom;
import com.linan.office.mapper.OfficeMeetingRoomMapper;
import com.linan.office.service.IOfficeMeetingBookingService;
import com.linan.office.service.IOfficeMeetingRoomService;
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
 * <p>覆盖核心业务规则：会议室存在性校验（行锁查询）、时间冲突拒绝、时间合法性。</p>
 */
@ExtendWith(MockitoExtension.class)
class OfficeMeetingBookingControllerTest {

    @Mock
    private IOfficeMeetingBookingService bookingService;

    @Mock
    private IOfficeMeetingRoomService roomService;

    @Mock
    private OfficeMeetingRoomMapper roomMapper;

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
    @DisplayName("会议室不存在时拒绝预订")
    void save_roomNotFound_rejected() {
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setRoomId(999L);
        booking.setTitle("周会");
        booking.setStartTime("2026-08-10 09:00");
        booking.setEndTime("2026-08-10 10:00");
        when(roomMapper.selectByIdForUpdate(999L)).thenReturn(null);

        Result<Void> result = controller.save(booking);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("会议室不存在"));
        verify(bookingService, never()).save(any());
    }

    @Test
    @DisplayName("时间冲突时拒绝预订")
    void save_timeConflict_rejected() {
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setRoomId(1L);
        booking.setTitle("周会");
        booking.setStartTime("2026-08-10 09:00");
        booking.setEndTime("2026-08-10 10:00");

        OfficeMeetingRoom room = new OfficeMeetingRoom();
        room.setId(1L);
        room.setRoomName("一号会议室");
        when(roomMapper.selectByIdForUpdate(1L)).thenReturn(room);

        // 模拟已存在一条重叠预订
        mockBookingConflict(1L);

        Result<Void> result = controller.save(booking);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("已被预订"));
        verify(bookingService, never()).save(any());
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
    }

    @Test
    @DisplayName("时间合法且无冲突时预订成功")
    void save_validBooking_success() {
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setRoomId(1L);
        booking.setTitle("项目评审会");
        booking.setStartTime("2026-08-10 14:00");
        booking.setEndTime("2026-08-10 15:00");
        booking.setBookerName("王五");

        OfficeMeetingRoom room = new OfficeMeetingRoom();
        room.setId(1L);
        room.setRoomName("一号会议室");
        when(roomMapper.selectByIdForUpdate(1L)).thenReturn(room);

        // 无时间冲突
        mockBookingConflict(0L);
        when(bookingService.save(any(OfficeMeetingBooking.class))).thenReturn(true);

        Result<Void> result = controller.save(booking);

        assertEquals(200, result.getCode());
        assertEquals("一号会议室", booking.getRoomName()); // 冗余字段自动填充
        assertEquals(1L, booking.getBookerId());            // 预订人自动填充
        verify(bookingService, times(1)).save(any());
    }

    /**
     * Mock bookingService.lambdaQuery() 链式调用，控制冲突查询返回的条数
     */
    @SuppressWarnings("unchecked")
    private void mockBookingConflict(long count) {
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<OfficeMeetingBooking> chain =
                mock(com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(bookingService.lambdaQuery()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.and(any())).thenReturn(chain);
        when(chain.count()).thenReturn(count);
    }
}
