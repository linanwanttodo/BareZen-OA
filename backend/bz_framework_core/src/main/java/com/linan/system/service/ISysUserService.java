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
     * 批量新增用户（每个账号均校验强密码策略与用户名唯一性，任一失败则整体回滚）
     *
     * @param users 用户列表（userName、password 必填，nickName 缺省取 userName）
     * @return 成功创建的数量
     */
    int batchAddUsers(List<SysUser> users);

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
