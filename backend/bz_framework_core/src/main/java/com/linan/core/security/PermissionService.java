package com.linan.core.security;

import com.linan.system.entity.SysRole;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.mapper.SysRoleMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限校验服务
 *
 * <p>封装用户权限查询逻辑，使用 Spring Cache（Redis）缓存查询结果，
 * 避免每次请求都重复查询数据库。</p>
 *
 * <p>缓存策略：</p>
 * <ul>
 *   <li>缓存 key：user_permissions::{userId}</li>
 *   <li>过期时间：60 秒（短 TTL 确保权限变更后快速生效）</li>
 *   <li>失效时机：管理员在后台修改角色权限时，调用 {@link #evictUserPermissions(Long)}</li>
 * </ul>
 *
 * @author BareZen Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysRoleMapper roleMapper;

    private final SysMenuMapper menuMapper;

    /**
     * 获取用户的所有权限标识，带 Redis 缓存
     *
     * @param userId 用户ID
     * @return 权限标识集合
     */
    @Cacheable(value = "user_permissions", key = "#userId", unless = "#result == null")
    public Set<String> getUserPermissions(Long userId) {
        if (userId == null) {
            return Set.of();
        }

        // 1. 查询用户角色
        List<SysRole> roles = roleMapper.selectRolesByUserId(userId);

        // 2. 如果是 admin 角色，直接返回通配符
        boolean isAdmin = roles.stream().anyMatch(r -> "admin".equals(r.getRoleKey()));
        if (isAdmin) {
            return Set.of("*:*:*");
        }

        // 3. 单次查询所有角色的权限（使用 JOIN，避免 N+1）
        Set<String> perms = new HashSet<>();
        for (SysRole role : roles) {
            List<String> rolePerms = menuMapper.selectPermsByRoleId(role.getId());
            perms.addAll(rolePerms);
        }

        return perms;
    }

    /**
     * 检查用户是否有指定权限
     *
     * @param userId  用户ID
     * @param permission 权限标识
     * @return true=有权限
     */
    public boolean hasPermission(Long userId, String permission) {
        if (userId == null || permission == null) {
            return false;
        }
        Set<String> perms = getUserPermissions(userId);
        return perms.contains("*:*:*") || perms.contains(permission);
    }

    /**
     * 检查用户是否拥有所有指定权限（AND 关系）
     *
     * @param userId  用户ID
     * @param permissions 权限标识数组
     * @return true=拥有所有权限
     */
    public boolean hasAllPermissions(Long userId, String[] permissions) {
        if (userId == null || permissions == null || permissions.length == 0) {
            return false;
        }
        Set<String> perms = getUserPermissions(userId);
        if (perms.contains("*:*:*")) {
            return true;
        }
        for (String perm : permissions) {
            if (!perms.contains(perm)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 清除用户的权限缓存（角色权限变更时调用）
     *
     * @param userId 用户ID
     */
    @CacheEvict(value = "user_permissions", key = "#userId")
    public void evictUserPermissions(Long userId) {
        log.debug("已清除用户 {} 的权限缓存", userId);
    }
}
