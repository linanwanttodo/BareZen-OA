package com.linan.web.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysMenu;
import com.linan.system.service.ISysMenuService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysMenuController 菜单管理 CRUD 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SysMenuControllerTest {

    @Mock
    private ISysMenuService menuService;

    private SysMenuController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new SysMenuController(menuService);
        setThreadLocal("currentUserIdHolder", 1L);
    }

    @AfterEach
    void tearDown() throws Exception {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("getRouters 返回当前用户路由菜单")
    void getRouters_returnsMenuTree() {
        List<SysMenu> menus = List.of(new SysMenu());
        when(menuService.selectMenuTree(1L)).thenReturn(menus);

        Result<List<SysMenu>> result = controller.getRouters();

        assertEquals(200, result.getCode());
        assertEquals(menus, result.getData());
        verify(menuService).selectMenuTree(1L);
    }

    @Test
    @DisplayName("treeselect 返回完整菜单树")
    void treeselect_returnsFullTree() {
        List<SysMenu> tree = List.of(new SysMenu());
        when(menuService.selectMenuTreeList()).thenReturn(tree);

        Result<List<SysMenu>> result = controller.treeselect();

        assertEquals(200, result.getCode());
        assertEquals(tree, result.getData());
        verify(menuService).selectMenuTreeList();
    }

    @Test
    @DisplayName("list 返回菜单列表")
    void list_returnsMenuList() {
        List<SysMenu> list = List.of(new SysMenu());
        when(menuService.selectMenuList()).thenReturn(list);

        Result<List<SysMenu>> result = controller.list();

        assertEquals(200, result.getCode());
        assertEquals(list, result.getData());
        verify(menuService).selectMenuList();
    }

    @Test
    @DisplayName("save 新增菜单成功")
    void save_success() {
        SysMenu menu = new SysMenu();
        when(menuService.save(any(SysMenu.class))).thenReturn(true);

        Result<Void> result = controller.save(menu);

        assertEquals(200, result.getCode());
        verify(menuService).save(menu);
    }

    @Test
    @DisplayName("save 新增菜单失败")
    void save_failed() {
        when(menuService.save(any(SysMenu.class))).thenReturn(false);

        Result<Void> result = controller.save(new SysMenu());

        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("update 更新菜单成功")
    void update_success() {
        SysMenu menu = new SysMenu();
        when(menuService.updateById(any(SysMenu.class))).thenReturn(true);

        Result<Void> result = controller.update(menu);

        assertEquals(200, result.getCode());
        verify(menuService).updateById(menu);
    }

    @Test
    @DisplayName("delete 有子菜单拒绝删除")
    void delete_hasChild_rejected() {
        when(menuService.countChildMenus(1L)).thenReturn(1L);

        Result<Void> result = controller.delete(1L);

        assertEquals(500, result.getCode());
        assertEquals("请先删除子菜单", result.getMsg());
    }

    @Test
    @DisplayName("delete 被角色引用拒绝删除")
    void delete_hasRoleRef_rejected() {
        when(menuService.countChildMenus(1L)).thenReturn(0L);
        when(menuService.countRoleMenu(1L)).thenReturn(1L);

        Result<Void> result = controller.delete(1L);

        assertEquals(500, result.getCode());
        assertEquals("该菜单已被角色引用，无法删除", result.getMsg());
    }

    @Test
    @DisplayName("delete 无子菜单无引用删除成功")
    void delete_success() {
        when(menuService.countChildMenus(1L)).thenReturn(0L);
        when(menuService.countRoleMenu(1L)).thenReturn(0L);
        when(menuService.removeById(1L)).thenReturn(true);

        Result<Void> result = controller.delete(1L);

        assertEquals(200, result.getCode());
        verify(menuService).removeById(1L);
    }
}