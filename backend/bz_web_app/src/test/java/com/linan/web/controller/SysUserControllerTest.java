package com.linan.web.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.Result;
import com.linan.system.entity.SysUser;
import com.linan.system.entity.SysUserDept;
import com.linan.system.mapper.SysUserMapper;
import com.linan.web.controller.dto.ResetPasswordRequest;
import com.linan.system.service.ISysUserService;
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
 * SysUserController 用户管理 CRUD 单元测试
 *
 * <p>SysUserController 继承 BaseController 提供通用 CRUD，本测试聚焦各业务端点
 * 的委托与参数传递。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysUserControllerTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private ISysUserService userService;

    private SysUserController controller;

    @BeforeEach
    void setUp() {
        controller = new SysUserController(userMapper, userService);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("分页查询用户列表默认参数")
    void list_defaultParams() {
        Page<SysUser> pageResult = new Page<>(1, 10);
        pageResult.setTotal(2);
        pageResult.setRecords(List.of(new SysUser(), new SysUser()));
        when(userService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageResult);

        Result<?> result = controller.list(1, 10, null, null, null);

        assertEquals(200, result.getCode());
        assertEquals(2, ((com.linan.core.common.api.PageResult<SysUser>) result.getData()).getTotal());
    }

    @Test
    @DisplayName("save 新增用户")
    void save_success() {
        SysUser user = new SysUser();

        Result<Void> result = controller.save(user);

        assertEquals(200, result.getCode());
        verify(userService).addUser(user);
    }

    @Test
    @DisplayName("batchSave 批量创建账号")
    void batchSave_success() {
        List<SysUser> users = List.of(new SysUser(), new SysUser());
        when(userService.batchAddUsers(users)).thenReturn(2);

        Result<Integer> result = controller.batchSave(users);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().intValue());
        verify(userService).batchAddUsers(users);
    }

    @Test
    @DisplayName("update 更新用户")
    void update_success() {
        SysUser user = new SysUser();

        Result<Void> result = controller.update(user);

        assertEquals(200, result.getCode());
        verify(userService).updateUser(user);
    }

    @Test
    @DisplayName("delete 删除用户成功")
    void delete_success() {
        when(userService.removeById(1L)).thenReturn(true);

        Result<Void> result = controller.delete(1L);

        assertEquals(200, result.getCode());
        verify(userService).removeById(1L);
    }

    @Test
    @DisplayName("delete 删除用户失败")
    void delete_failed() {
        when(userService.removeById(1L)).thenReturn(false);

        Result<Void> result = controller.delete(1L);

        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("deleteBatch 批量删除用户")
    void deleteBatch_success() {
        List<Long> ids = List.of(1L, 2L);
        when(userService.removeByIds(ids)).thenReturn(true);

        Result<Void> result = controller.deleteBatch(ids);

        assertEquals(200, result.getCode());
        verify(userService).removeByIds(ids);
    }

    @Test
    @DisplayName("changeStatus 切换用户状态")
    void changeStatus_success() {
        when(userService.updateById(any(SysUser.class))).thenReturn(true);

        Result<Void> result = controller.changeStatus(1L, "1");

        assertEquals(200, result.getCode());
        verify(userService).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("resetPassword 重置密码成功")
    void resetPassword_success() {
        when(userService.resetPassword(1L, "newPass123!")).thenReturn(null);

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setPassword("newPass123!");

        Result<Void> result = controller.resetPassword(1L, req);

        assertEquals(200, result.getCode());
        verify(userService).resetPassword(1L, "newPass123!");
    }

    @Test
    @DisplayName("resetPassword 重置密码失败")
    void resetPassword_failed() {
        when(userService.resetPassword(1L, "weak")).thenReturn("密码不符合策略要求");

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setPassword("weak");

        Result<Void> result = controller.resetPassword(1L, req);

        assertEquals(500, result.getCode());
        assertEquals("密码不符合策略要求", result.getMsg());
    }

    @Test
    @DisplayName("getUserRoles 获取用户角色ID列表")
    void getUserRoles() {
        when(userService.getUserRoleIds(1L)).thenReturn(List.of(1L, 2L));

        Result<List<Long>> result = controller.getUserRoles(1L);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
        verify(userService).getUserRoleIds(1L);
    }

    @Test
    @DisplayName("assignRoles 分配用户角色")
    void assignRoles() {
        List<Long> roleIds = List.of(1L, 2L);

        Result<Void> result = controller.assignRoles(1L, roleIds);

        assertEquals(200, result.getCode());
        verify(userService).assignRoles(1L, roleIds);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("filterUsers 按角色部门筛选用户")
    void filterUsers() {
        Page<SysUser> pageResult = new Page<>(1, 10);
        pageResult.setTotal(1);
        pageResult.setRecords(List.of(new SysUser()));
        when(userMapper.selectUsersByRoleDeptPage(any(Page.class), any(), any(), any(), any()))
                .thenReturn(pageResult);

        Result<?> result = controller.filterUsers(null, null, null, null, 1, 10);

        assertEquals(200, result.getCode());
        assertEquals(1, ((com.linan.core.common.api.PageResult<SysUser>) result.getData()).getTotal());
    }

    @Test
    @DisplayName("getUserDepts 获取用户部门列表")
    void getUserDepts() {
        List<SysUserDept> depts = List.of(new SysUserDept());
        when(userService.getUserDepts(1L)).thenReturn(depts);

        Result<List<SysUserDept>> result = controller.getUserDepts(1L);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        verify(userService).getUserDepts(1L);
    }

    @Test
    @DisplayName("assignDepts 分配用户部门")
    void assignDepts() {
        List<SysUserDept> deptList = List.of(new SysUserDept());

        Result<Void> result = controller.assignDepts(1L, deptList);

        assertEquals(200, result.getCode());
        verify(userService).assignDepts(1L, deptList);
    }
}