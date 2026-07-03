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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统用户 Service 实现
 *
 * 封装用户管理的核心业务逻辑，
 * Controller 层只需调用 Service 方法，不直接操作 Mapper。
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final SysUserRoleMapper userRoleMapper;

    private final SysUserDeptMapper userDeptMapper;

    @Override
    public SysUser getUserByUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return null;
        }
        return baseMapper.selectOne(
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
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, user.getUserName().trim()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 密码加密前先做强密码策略校验
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String policyMsg = com.linan.core.security.PasswordUtil.validatePasswordPolicy(user.getPassword());
            if (policyMsg != null) {
                throw new BusinessException(policyMsg);
            }
            user.setPassword(PasswordUtil.encode(user.getPassword()));
        }

        // 状态默认值
        if (user.getStatus() == null) {
            user.setStatus("0");
        }

        int rows = baseMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException("新增用户失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAddUsers(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            throw new BusinessException("请至少提供一条用户数据");
        }
        if (users.size() > 200) {
            throw new BusinessException("单次批量创建不能超过200条");
        }

        // 先整体校验、再统一写入：此前逐条 selectCount 查库（N+1）并用 subList 两两比对
        // 列表内重复（O(n²)）；改为一次 IN 查询 + 一个 Set 完成同等校验，语义与文案不变
        Set<String> names = new LinkedHashSet<>();
        for (SysUser user : users) {
            if (user == null) {
                throw new BusinessException("存在空用户数据，已回滚");
            }
            if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
                throw new BusinessException("存在用户名为空的记录，已回滚");
            }
            // 强密码策略校验（与注册/改密一致）
            String policyMsg = com.linan.core.security.PasswordUtil.validatePasswordPolicy(user.getPassword());
            if (policyMsg != null) {
                throw new BusinessException("用户 [" + user.getUserName() + "] " + policyMsg + "，已回滚");
            }
            if (!names.add(user.getUserName().trim())) {
                throw new BusinessException("列表中存在重复用户名 [" + user.getUserName() + "]，已回滚");
            }
        }

        List<SysUser> taken = baseMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getUserName)
                        .in(SysUser::getUserName, names));
        if (!taken.isEmpty()) {
            throw new BusinessException("用户名 [" + taken.get(0).getUserName() + "] 已存在，已回滚");
        }

        int created = 0;
        for (SysUser user : users) {
            // 昵称缺省取用户名
            if (user.getNickName() == null || user.getNickName().isBlank()) {
                user.setNickName(user.getUserName().trim());
            }
            if (user.getStatus() == null) {
                user.setStatus("0");
            }
            user.setPassword(PasswordUtil.encode(user.getPassword()));
            int rows = baseMapper.insert(user);
            if (rows <= 0) {
                throw new BusinessException("新增用户失败，已回滚");
            }
            created++;
        }
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException("用户信息不完整");
        }

        // 如果传了密码，先做强密码策略校验再加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String policyMsg = com.linan.core.security.PasswordUtil.validatePasswordPolicy(user.getPassword());
            if (policyMsg != null) {
                throw new BusinessException(policyMsg);
            }
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
        // 强密码策略：长度 >= 8 位，大小写字母+数字+特殊字符（与注册/改密接口一致）
        String policyMsg = com.linan.core.security.PasswordUtil.validatePasswordPolicy(newPassword);
        if (policyMsg != null) {
            throw new BusinessException(policyMsg);
        }

        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }

        // 更新新密码
        user.setPassword(PasswordUtil.encode(newPassword));
        baseMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(Long id, String password) {
        if (password == null || password.isEmpty()) {
            return "新密码不能为空";
        }
        String policyMsg = PasswordUtil.validatePasswordPolicy(password);
        if (policyMsg != null) {
            return policyMsg;
        }
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            return "用户不存在";
        }
        user.setPassword(PasswordUtil.encode(password));
        return updateById(user) ? null : "重置失败";
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

            // 一次批量 insert，替代循环逐条插入
            int rows = userRoleMapper.insertBatch(list);
            if (rows <= 0) {
                throw new BusinessException("分配角色失败");
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
            }
            // 一次批量 insert，替代循环逐条插入
            int rows = userDeptMapper.insertBatch(deptList);
            if (rows <= 0) {
                throw new BusinessException("分配部门失败");
            }
        }
    }
}