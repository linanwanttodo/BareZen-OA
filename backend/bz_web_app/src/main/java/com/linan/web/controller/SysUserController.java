package com.linan.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.controller.BaseController;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysUser;
import com.linan.system.entity.SysUserDept;
import com.linan.system.mapper.SysUserMapper;
import com.linan.system.service.ISysUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 系统用户 Controller
 *
 * 职责：参数校验、请求路由、调用 Service 层
 * Service 层负责具体业务逻辑
 */
@Tag(name = "用户管理", description = "系统用户 CRUD")
@RestController
@RequestMapping("/api/system/v1/users")
public class SysUserController extends BaseController<SysUser, ISysUserService> {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ISysUserService userService;

    @Override
    protected ISysUserService getService() {
        return userService;
    }

    /**
     * 条件分页查询
     */
    @RequirePermission("system:user:list")
    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String phone) {

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (userName != null && !userName.isEmpty()) {
            wrapper.like(SysUser::getUserName, userName);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (phone != null && !phone.isEmpty()) {
            wrapper.like(SysUser::getPhone, phone);
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> result = userService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 新增用户
     */
    @Operation(summary = "新增用户")
    @OperLog(title = "用户管理", businessType = 1)
    @RequirePermission("system:user:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody SysUser user) {
        userService.addUser(user);
        return Result.success(null);
    }

    /**
     * 批量创建账号（每个账号均校验强密码策略与用户名唯一性，任一失败整体回滚）
     */
    @Operation(summary = "批量创建账号")
    @OperLog(title = "用户管理", businessType = 1)
    @RequirePermission("system:user:add")
    @PostMapping("/batch")
    public Result<Integer> batchSave(@RequestBody List<SysUser> users) {
        int created = userService.batchAddUsers(users);
        return Result.success(created);
    }

    /**
     * 更新用户
     */
    @Operation(summary = "修改用户")
    @OperLog(title = "用户管理", businessType = 2)
    @RequirePermission("system:user:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysUser user) {
        userService.updateUser(user);
        return Result.success(null);
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户")
    @OperLog(title = "用户管理", businessType = 3)
    @RequirePermission("system:user:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = userService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 批量删除
     */
    @Operation(summary = "批量删除用户")
    @RequirePermission("system:user:delete")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        boolean ok = userService.removeByIds(ids);
        return ok ? Result.success(null) : Result.error("批量删除失败");
    }

    /**
     * 切换用户状态
     */
    @Operation(summary = "切换用户状态")
    @RequirePermission("system:user:edit")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        boolean ok = userService.updateById(user);
        return ok ? Result.success(null) : Result.error("状态切换失败");
    }

    // ============ 角色分配 ============

    /**
     * 获取用户已分配的角色ID列表
     */
    @Operation(summary = "获取用户角色")
    @RequirePermission("system:user:list")
    @GetMapping("/{userId}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long userId) {
        List<Long> roleIds = userService.getUserRoleIds(userId);
        return Result.success(roleIds);
    }

    /**
     * 分配用户角色（全量覆盖）
     */
    @Operation(summary = "分配用户角色")
    @RequirePermission("system:user:edit")
    @PutMapping("/{userId}/roles")
    public Result<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        userService.assignRoles(userId, roleIds);
        return Result.success(null);
    }

    // ============ 角色/部门 联合筛选 ============

    /**
     * 按角色和/或部门筛选用户（分页）
     * 使用 MyBatis-Plus Page 实现数据库层 LIMIT 分页
     */
    @RequirePermission("system:user:list")
    @GetMapping("/filter")
    public Result<PageResult<SysUser>> filterUsers(
            @RequestParam(required = false) String roleIds,
            @RequestParam(required = false) String deptIds,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        List<Long> roleIdList = Optional.ofNullable(roleIds)
                .filter(s -> !s.isEmpty())
                .map(s -> Arrays.stream(s.split(","))
                        .map(Long::parseLong).collect(Collectors.toList()))
                .orElse(null);
        List<Long> deptIdList = Optional.ofNullable(deptIds)
                .filter(s -> !s.isEmpty())
                .map(s -> Arrays.stream(s.split(","))
                        .map(Long::parseLong).collect(Collectors.toList()))
                .orElse(null);

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        Page<SysUser> resultPage = userMapper.selectUsersByRoleDeptPage(page, roleIdList, deptIdList, userName, status);

        // 统一分页返回结构（list 字段），与前端 PageData 类型约定一致
        return Result.page(resultPage);
    }

    // ============ 部门分配 ============

    /**
     * 获取用户所属部门列表
     */
    @RequirePermission("system:user:list")
    @GetMapping("/{userId}/depts")
    public Result<List<SysUserDept>> getUserDepts(@PathVariable Long userId) {
        List<SysUserDept> list = userService.getUserDepts(userId);
        return Result.success(list);
    }

    /**
     * 分配用户部门（全量覆盖）
     */
    @RequirePermission("system:user:edit")
    @PutMapping("/{userId}/depts")
    public Result<Void> assignDepts(@PathVariable Long userId, @RequestBody List<SysUserDept> deptList) {
        userService.assignDepts(userId, deptList);
        return Result.success(null);
    }
}