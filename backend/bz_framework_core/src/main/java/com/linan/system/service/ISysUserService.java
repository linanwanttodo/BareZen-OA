package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysUser;
import com.linan.system.entity.SysUserDept;

import java.util.List;

/**
 * 系统用户 Service 接口
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser getUserByUserName(String userName);

    /**
     * 新增用户（含密码加密、用户名唯一校验）
     *
     * @param user 用户信息（含明文密码）
     */
    void addUser(SysUser user);

    /**
     * 更新用户（可选密码加密）
     */
    void updateUser(SysUser user);

    /**
     * 修改密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 获取用户已分配的角色ID列表
     */
    List<Long> getUserRoleIds(Long userId);

    /**
     * 分配用户角色（全量覆盖）
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 获取用户所属部门列表
     */
    List<SysUserDept> getUserDepts(Long userId);

    /**
     * 分配用户部门（全量覆盖）
     */
    void assignDepts(Long userId, List<SysUserDept> deptList);
}
