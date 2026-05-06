package com.linan.system.spi;

import com.linan.core.spi.permission.PermissionGateway;
import com.linan.system.entity.SysRole;
import com.linan.system.mapper.SysDeptMapper;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.mapper.SysRoleMapper;
import com.linan.system.mapper.SysUserDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限/数据权限 SPI 的系统侧实现
 *
 * <p>将 core 的权限契约映射到 sys_role/sys_menu/sys_dept/sys_user_dept 查询，
 * 使 {@code com.linan.core.security} 不再直接依赖系统 mapper。</p>
 *
 * @author BareZen Team
 */
@Component
@RequiredArgsConstructor
public class PermissionGatewayImpl implements PermissionGateway {

    /** 超管角色键 */
    private static final String ROLE_KEY_ADMIN = "admin";

    /** 数据权限：全部 */
    private static final int SCOPE_ALL = 4;

    private final SysRoleMapper roleMapper;

    private final SysMenuMapper menuMapper;

    private final SysDeptMapper deptMapper;

    private final SysUserDeptMapper userDeptMapper;

    private List<SysRole> rolesOf(Long userId) {
        return userId == null ? List.of() : roleMapper.selectRolesByUserId(userId);
    }

    @Override
    public boolean isAdmin(Long userId) {
        return rolesOf(userId).stream()
                .anyMatch(r -> ROLE_KEY_ADMIN.equals(r.getRoleKey()));
    }

    @Override
    public Set<String> collectRolePermissions(Long userId) {
        Set<String> perms = new HashSet<>();
        for (SysRole role : rolesOf(userId)) {
            perms.addAll(menuMapper.selectPermsByRoleId(role.getId()));
        }
        return perms;
    }

    @Override
    public int minDataScope(Long userId) {
        return rolesOf(userId).stream()
                .filter(r -> r.getDataScope() != null)
                .mapToInt(SysRole::getDataScope)
                .min()
                .orElse(SCOPE_ALL);
    }

    @Override
    public List<Long> selectDeptIdsByUserId(Long userId) {
        return userDeptMapper.selectDeptIdsByUserId(userId);
    }

    @Override
    public List<Long> selectChildDeptIds(Long parentId) {
        return deptMapper.selectDeptIdsByParentId(parentId);
    }
}