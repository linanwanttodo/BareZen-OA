package com.linan.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 权限辅助工具
 *
 * <p>收敛各模块 Controller/service 中重复的「本人（owner）或持权限（admin）」双轨放行判定，
 * 避免散落多处 {@code isOwner || hasPermission} 拷贝导致的语义漂移。</p>
 */
@Component
@RequiredArgsConstructor
public class PermissionSupport {

    private final PermissionService permissionService;

    /**
     * 判断用户是否为资源所有者，或持有指定权限。
     *
     * @param userId     当前用户 ID（可空，视为无权限）
     * @param ownerId    资源所有者 ID
     * @param permission 兜底权限标识
     * @return true=本人或持权限
     */
    public boolean isOwnerOrHasPermission(Long userId, Long ownerId, String permission) {
        if (userId == null) {
            return false;
        }
        boolean isOwner = userId.equals(ownerId);
        return isOwner || permissionService.hasPermission(userId, permission);
    }

    /**
     * 判断用户是否持有指定管理权限。
     *
     * @param userId     当前用户 ID（可空，视为无权限）
     * @param permission 权限标识（如 hr:leave:audit）
     * @return true=持权限
     */
    public boolean hasPermission(Long userId, String permission) {
        if (userId == null) {
            return false;
        }
        return permissionService.hasPermission(userId, permission);
    }
}