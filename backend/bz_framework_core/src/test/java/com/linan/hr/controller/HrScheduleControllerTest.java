package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.HrSchedule;
import com.linan.hr.service.IHrScheduleService;
import com.linan.system.entity.SysUser;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端日程新增/修改 Controller 单元测试（2.4）
 *
 * <p>覆盖：新增委托 resolveAndCreate（userName→userId 回填与回落已下沉至 Service，
 * 由 HrScheduleServiceImplTest 覆盖）、时间倒置拒绝、修改不存在日程拒绝、合法更新。\u003c/p>
 */
@ExtendWith(MockitoExtension.class)
class HrScheduleControllerTest {

    @Mock
    private IHrScheduleService scheduleService;

    @InjectMocks
    private HrScheduleController controller;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), HrSchedule.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysUser.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        setThreadLocal("currentUsernameHolder", "admin");
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    private HrSchedule schedule(String title, String start, String end) {
        HrSchedule s = new HrSchedule();
        s.setTitle(title);
        s.setStartTime(start);
        s.setEndTime(end);
        return s;
    }

    @Test
    @DisplayName("新增：委托 resolveAndCreate（userName→userId 关联回填在 Service 内完成）")
    void save_delegatesToResolveAndCreate() {
        HrSchedule s = schedule("项目周会", "2026-08-25 10:00", "2026-08-25 11:00");
        s.setUserName("张三");
        when(scheduleService.resolveAndCreate(s)).thenReturn(true);

        Result<Void> result = controller.save(s);

        assertEquals(200, result.getCode());
        verify(scheduleService).resolveAndCreate(s);
    }

    @Test
    @DisplayName("新增：userName 回填失败回落当前登录用户逻辑在 Service 内（此处仅委托）")
    void save_userNameNotFound_fallbackInService() {
        HrSchedule s = schedule("项目周会", "2026-08-25 10:00", "2026-08-25 11:00");
        s.setUserName("不存在的人");
        when(scheduleService.resolveAndCreate(s)).thenReturn(true);

        Result<Void> result = controller.save(s);

        assertEquals(200, result.getCode());
        verify(scheduleService).resolveAndCreate(s);
    }

    @Test
    @DisplayName("新增：开始时间不早于结束时间被拒绝")
    void save_timeInverted_rejected() {
        HrSchedule s = schedule("非法日程", "2026-08-25 11:00", "2026-08-25 10:00");

        Result<Void> result = controller.save(s);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("结束时间"));
        verify(scheduleService, never()).resolveAndCreate(any());
    }

    @Test
    @DisplayName("修改：不存在日程被拒绝")
    void update_missingSchedule_rejected() {
        HrSchedule s = schedule("改期", "2026-08-25 14:00", "2026-08-25 15:00");
        when(scheduleService.getById(10L)).thenReturn(null);

        Result<Void> result = controller.update(10L, s);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不存在"));
        verify(scheduleService, never()).updateById(any());
    }

    @Test
    @DisplayName("修改：合法更新成功且 id 生效")
    void update_valid() {
        HrSchedule s = schedule("改期", "2026-08-25 14:00", "2026-08-25 15:00");
        when(scheduleService.getById(10L)).thenReturn(new HrSchedule());
        when(scheduleService.updateById(any(HrSchedule.class))).thenReturn(true);

        Result<Void> result = controller.update(10L, s);

        assertEquals(200, result.getCode());
        assertEquals(10L, s.getId());
        assertNotNull(s.getId());
    }
}