package com.linan.web.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.system.entity.SysRole;
import com.linan.system.service.ISysRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysRoleController 角色管理 CRUD 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SysRoleControllerTest {

    @Mock
    private ISysRoleService roleService;

    private SysRoleController controller;

    @BeforeEach
    void setUp() {
        controller = new SysRoleController(roleService);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("分页查询角色列表")
    void list_defaultParams() {
        Page<SysRole> pageResult = new Page<>(1, 10);
        pageResult.setTotal(2);
        pageResult.setRecords(List.of(new SysRole(), new SysRole()));
        when(roleService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageResult);

        Result<PageResult<SysRole>> result = controller.list(1, 10, null, null, null);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().getTotal());
        assertEquals(2, result.getData().getList().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("分页查询按角色名模糊搜索")
    void list_withRoleName() {
        Page<SysRole> pageResult = new Page<>(1, 10);
        pageResult.setTotal(1);
        pageResult.setRecords(List.of(new SysRole()));
        when(roleService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageResult);

        Result<PageResult<SysRole>> result = controller.list(1, 10, "admin", null, null);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    @DisplayName("save 新增角色成功")
    void save_success() {
        SysRole role = new SysRole();
        role.setRoleKey("test");
        when(roleService.checkRoleKeyUnique("test")).thenReturn(true);
        when(roleService.save(any(SysRole.class))).thenReturn(true);

        Result<Void> result = controller.save(role);

        assertEquals(200, result.getCode());
        verify(roleService).save(role);
    }

    @Test
    @DisplayName("save 角色标识重复拒绝")
    void save_duplicateRoleKey() {
        SysRole role = new SysRole();
        role.setRoleKey("test");
        when(roleService.checkRoleKeyUnique("test")).thenReturn(false);

        Result<Void> result = controller.save(role);

        assertEquals(500, result.getCode());
        assertEquals("角色标识已存在", result.getMsg());
    }

    @Test
    @DisplayName("update 更新角色成功")
    void update_success() {
        SysRole role = new SysRole();
        when(roleService.updateById(any(SysRole.class))).thenReturn(true);

        Result<Void> result = controller.update(role);

        assertEquals(200, result.getCode());
        verify(roleService).updateById(role);
    }

    @Test
    @DisplayName("delete 删除角色成功")
    void delete_success() {
        when(roleService.removeById(1L)).thenReturn(true);

        Result<Void> result = controller.delete(1L);

        assertEquals(200, result.getCode());
        verify(roleService).removeById(1L);
    }

    @Test
    @DisplayName("deleteBatch 批量删除角色")
    void deleteBatch_success() {
        List<Long> ids = List.of(1L, 2L);
        when(roleService.removeByIds(ids)).thenReturn(true);

        Result<Void> result = controller.deleteBatch(ids);

        assertEquals(200, result.getCode());
        verify(roleService).removeByIds(ids);
    }

    @Test
    @DisplayName("assignMenus 分配菜单权限")
    void assignMenus_success() {
        List<Long> menuIds = List.of(1L, 2L);

        Result<Void> result = controller.assignMenus(1L, menuIds);

        assertEquals(200, result.getCode());
        verify(roleService).assignRoleMenus(1L, menuIds);
    }

    @Test
    @DisplayName("getRoleMenuIds 查询角色菜单ID列表")
    void getRoleMenuIds() {
        when(roleService.getMenuIdsByRoleId(1L)).thenReturn(List.of(1L, 2L));

        Result<List<Long>> result = controller.getRoleMenuIds(1L);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
        verify(roleService).getMenuIdsByRoleId(1L);
    }

    @Test
    @DisplayName("changeStatus 切换角色状态")
    void changeStatus_success() {
        when(roleService.updateById(any(SysRole.class))).thenReturn(true);

        Result<Void> result = controller.changeStatus(1L, "1");

        assertEquals(200, result.getCode());
        verify(roleService).updateById(any(SysRole.class));
    }
}