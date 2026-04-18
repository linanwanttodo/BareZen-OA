package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.PasswordUtil;
import com.linan.system.entity.SysUser;
import com.linan.system.entity.SysUserDept;
import com.linan.system.entity.SysUserRole;
import com.linan.system.mapper.SysUserDeptMapper;
import com.linan.system.mapper.SysUserMapper;
import com.linan.system.mapper.SysUserRoleMapper;
import com.linan.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统用户 Service 实现
 *
 * 封装用户管理的核心业务逻辑，
 * Controller 层只需调用 Service 方法，不直接操作 Mapper。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysUserDeptMapper userDeptMapper;

    @Override
    public SysUser getUserByUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return null;
        }
        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, userName.trim()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(SysUser user) {
        if (user == null) {
            throw new BusinessException("用户信息不能为空");
        }
        if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }

        // 检查用户名唯一
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, user.getUserName().trim()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 密码加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordUtil.encode(user.getPassword()));
        }

        // 状态默认值
        if (user.getStatus() == null) {
            user.setStatus("0");
        }

        int rows = userMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException("新增用户失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException("用户信息不完整");
        }

        // 如果传了密码，加密（以 $2a$ 开头说明已是加密密码，跳过）
        if (user.getPassword() != null && !user.getPassword().isEmpty()
                && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(PasswordUtil.encode(user.getPassword()));
        } else {
            // 不更新密码字段
            user.setPassword(null);
        }

        boolean ok = this.updateById(user);
        if (!ok) {
            throw new BusinessException("更新用户失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new BusinessException("旧密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BusinessException("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }

        // 更新新密码
        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userRoleMapper.selectByUserId(userId)
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    public List<SysUserDept> getUserDepts(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userDeptMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        // 1. 删除原有角色关联
        userRoleMapper.deleteByUserId(userId);

        // 2. 批量插入新角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysUserRole> list = roleIds.stream().map(roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                return ur;
            }).collect(Collectors.toList());

            for (SysUserRole ur : list) {
                int rows = userRoleMapper.insert(ur);
                if (rows <= 0) {
                    throw new BusinessException("分配角色失败");
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDepts(Long userId, List<SysUserDept> deptList) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        // 1. 删除原有部门关联
        userDeptMapper.deleteByUserId(userId);

        // 2. 批量插入新关联
        if (deptList != null && !deptList.isEmpty()) {
            for (SysUserDept d : deptList) {
                d.setUserId(userId);
                int rows = userDeptMapper.insert(d);
                if (rows <= 0) {
                    throw new BusinessException("分配部门失败");
                }
            }
        }
    }
}