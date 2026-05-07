package com.linan.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.HrSchedule;
import com.linan.hr.mapper.HrScheduleMapper;
import com.linan.system.entity.SysUser;
import com.linan.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HrScheduleServiceImpl 单元测试：userName→userId 回填、userId→昵称回显下沉后的行为
 */
@ExtendWith(MockitoExtension.class)
class HrScheduleServiceImplTest {

    @Mock
    private HrScheduleMapper mapper;
    @Mock
    private ISysUserService userService;

    private HrScheduleServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new HrScheduleServiceImpl(userService);
        Field baseMapper = HrScheduleServiceImpl.class.getSuperclass().getDeclaredField("baseMapper");
        baseMapper.setAccessible(true);
        baseMapper.set(service, mapper);

        setCurrentUser(3L);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setCurrentUser(Long userId) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField("currentUserIdHolder");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.lang.ThreadLocal<Object> tl = (java.lang.ThreadLocal<Object>) field.get(null);
        tl.set(userId);
    }

    @Test
    @DisplayName("resolveAndCreate：按 userName 关联回填 userId")
    void resolveAndCreate_byUserName() {
        HrSchedule schedule = new HrSchedule();
        schedule.setUserName("zhangsan");
        SysUser user = new SysUser();
        user.setId(42L);
        when(userService.list(any(Wrapper.class))).thenReturn(List.of(user));
        doReturn(1).when(mapper).insert(any(HrSchedule.class));

        service.resolveAndCreate(schedule);

        assertEquals(42L, schedule.getUserId());
        verify(mapper).insert(any(HrSchedule.class));
    }

    @Test
    @DisplayName("resolveAndCreate：userName 缺省时回退当前登录用户")
    void resolveAndCreate_fallbackCurrent() {
        HrSchedule schedule = new HrSchedule();
        doReturn(1).when(mapper).insert(any(HrSchedule.class));

        service.resolveAndCreate(schedule);

        assertEquals(3L, schedule.getUserId());
    }

    @Test
    @DisplayName("pageWithUserName：按 userId 回显昵称")
    void pageWithUserName_fillsNickName() {
        HrSchedule s = new HrSchedule();
        s.setUserId(8L);
        IPage<HrSchedule> page = new Page<>();
        page.setRecords(List.of(s));
        doReturn(page).when(mapper).selectPage(any(Page.class), any(Wrapper.class));

        SysUser u = new SysUser();
        u.setId(8L);
        u.setNickName("李四");
        lenient().when(userService.listByIds(any())).thenReturn(List.of(u));

        IPage<HrSchedule> result = service.pageWithUserName(new Page<>(), new LambdaQueryWrapper<>());
        assertEquals("李四", result.getRecords().get(0).getUserName());
    }
}