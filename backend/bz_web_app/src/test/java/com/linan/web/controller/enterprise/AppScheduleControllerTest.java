package com.linan.web.controller.enterprise;

import com.linan.core.common.api.Result;
import com.linan.core.security.PermissionService;
import com.linan.core.security.PermissionSupport;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.HrSchedule;
import com.linan.hr.service.IHrScheduleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppScheduleController 删除日程所有权双轨校验单测
 *
 * <p>覆盖 T13：本人 OR 持 hr:schedule:delete 权限的管理员均可删除；否则拒绝且不触库。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppScheduleControllerTest {

    /** sys_menu 中已存在的日程删除权限标识（V26 菜单 id=10245，禁止发明新码） */
    private static final String SCHEDULE_DELETE_PERM = "hr:schedule:delete";

    @Mock
    private IHrScheduleService scheduleService;

    @Mock
    private PermissionService permissionService;

    private PermissionSupport permissionSupport;

    private AppScheduleController controller;

    @BeforeEach
    void setUp() {
        permissionSupport = new PermissionSupport(permissionService);
        controller = new AppScheduleController(scheduleService, permissionSupport);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setCurrentUser(Long userId) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField("currentUserIdHolder");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(userId);
    }

    @Test
    @DisplayName("作者本人删除自己的日程放行")
    void deleteSchedule_owner_allowed() throws Exception {
        setCurrentUser(1L);
        HrSchedule schedule = new HrSchedule();
        schedule.setId(10L);
        schedule.setUserId(1L);
        when(scheduleService.getById(10L)).thenReturn(schedule);
        when(scheduleService.removeById(10L)).thenReturn(true);

        Result<Void> result = controller.deleteSchedule(10L);

        assertEquals(200, result.getCode());
        verify(scheduleService).removeById(10L);
    }

    @Test
    @DisplayName("非作者且无权限：拒绝且不触库")
    void deleteSchedule_notOwnerNoPermission_rejected() throws Exception {
        setCurrentUser(2L);
        HrSchedule schedule = new HrSchedule();
        schedule.setId(11L);
        schedule.setUserId(1L);
        when(scheduleService.getById(11L)).thenReturn(schedule);
        when(permissionService.hasPermission(2L, SCHEDULE_DELETE_PERM)).thenReturn(false);

        Result<Void> result = controller.deleteSchedule(11L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("无权限"));
        verify(scheduleService, never()).removeById(11L);
    }

    @Test
    @DisplayName("非作者但持删除权限（管理员旁路）：放行")
    void deleteSchedule_notOwnerWithPermission_allowed() throws Exception {
        setCurrentUser(2L);
        HrSchedule schedule = new HrSchedule();
        schedule.setId(12L);
        schedule.setUserId(1L);
        when(scheduleService.getById(12L)).thenReturn(schedule);
        when(permissionService.hasPermission(2L, SCHEDULE_DELETE_PERM)).thenReturn(true);
        when(scheduleService.removeById(12L)).thenReturn(true);

        Result<Void> result = controller.deleteSchedule(12L);

        assertEquals(200, result.getCode());
        verify(scheduleService).removeById(12L);
    }

    @Test
    @DisplayName("日程不存在返回错误")
    void deleteSchedule_notFound_rejected() throws Exception {
        setCurrentUser(1L);
        when(scheduleService.getById(99L)).thenReturn(null);

        Result<Void> result = controller.deleteSchedule(99L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不存在"));
        verify(scheduleService, never()).removeById(99L);
    }
}