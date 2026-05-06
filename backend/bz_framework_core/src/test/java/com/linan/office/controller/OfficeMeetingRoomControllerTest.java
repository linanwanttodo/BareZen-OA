package com.linan.office.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeMeetingRoom;
import com.linan.office.service.IOfficeMeetingRoomService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会议室管理 Controller 单元测试
 *
 * <p>覆盖：分页/可用列表的条件过滤、新增缺省状态、CRUD 成功/失败分支。</p>
 */
@ExtendWith(MockitoExtension.class)
class OfficeMeetingRoomControllerTest {

    @Mock
    private IOfficeMeetingRoomService roomService;

    @InjectMocks
    private OfficeMeetingRoomController controller;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OfficeMeetingRoom.class);
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

    private OfficeMeetingRoom room(String name) {
        OfficeMeetingRoom r = new OfficeMeetingRoom();
        r.setRoomName(name);
        return r;
    }

    @Test
    @DisplayName("分页：会议室名称条件透传为 like")
    void list_roomNameFilterApplied() {
        Page<OfficeMeetingRoom> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(roomService.page(any(Page.class), any(Wrapper.class))).thenReturn(page);

        controller.list(1, 10, "一号", null);

        verify(roomService).page(any(Page.class),
                argThat(w -> w.getTargetSql().contains("room_name")));
    }

    @Test
    @DisplayName("分页：状态条件透传为 eq")
    void list_statusFilterApplied() {
        Page<OfficeMeetingRoom> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(roomService.page(any(Page.class), any(Wrapper.class))).thenReturn(page);

        controller.list(1, 10, null, "0");

        verify(roomService).page(any(Page.class),
                argThat(w -> w.getTargetSql().contains("status")));
    }

    @Test
    @DisplayName("可用列表：仅返回启用（status=0）会议室")
    void available_onlyEnabledRooms() {
        OfficeMeetingRoom r1 = room("一号会议室");
        when(roomService.list(any(Wrapper.class))).thenReturn(List.of(r1));

        Result<List<OfficeMeetingRoom>> result = controller.available();

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        verify(roomService).list(argThat((Wrapper<OfficeMeetingRoom> w) -> w.getTargetSql().contains("status")));
    }

    @Test
    @DisplayName("新增：状态缺省默认启用（0）")
    void save_defaultStatusApplied() {
        OfficeMeetingRoom r = room("一号会议室");
        when(roomService.save(any(OfficeMeetingRoom.class))).thenReturn(true);

        Result<Void> result = controller.save(r);

        assertEquals(200, result.getCode());
        assertEquals("0", r.getStatus(), "缺省状态应为启用");
    }

    @Test
    @DisplayName("新增：保存失败返回错误")
    void save_failure() {
        OfficeMeetingRoom r = room("一号会议室");
        r.setStatus("0");
        when(roomService.save(any(OfficeMeetingRoom.class))).thenReturn(false);

        Result<Void> result = controller.save(r);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("失败"));
    }

    @Test
    @DisplayName("修改：成功且 id 转发")
    void update_success() {
        OfficeMeetingRoom r = room("二号会议室");
        when(roomService.updateById(any(OfficeMeetingRoom.class))).thenReturn(true);

        Result<Void> result = controller.update(r);

        assertEquals(200, result.getCode());
        verify(roomService).updateById(r);
    }

    @Test
    @DisplayName("删除：成功")
    void delete_success() {
        when(roomService.removeById(3L)).thenReturn(true);

        Result<Void> result = controller.delete(3L);

        assertEquals(200, result.getCode());
        verify(roomService).removeById(3L);
    }
}