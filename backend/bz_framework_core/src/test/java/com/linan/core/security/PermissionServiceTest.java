package com.linan.core.security;

import com.linan.core.spi.permission.PermissionGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PermissionService 单元测试（经 PermissionGateway SPI 隔离系统查询）
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionGateway permissionGateway;

    @InjectMocks
    private PermissionService permissionService;

    @Nested
    @DisplayName("getUserPermissions()")
    class GetUserPermissions {

        @Test
        @DisplayName("userId 为 null 时返回空集合")
        void nullUserId_returnsEmptySet() {
            Set<String> result = permissionService.getUserPermissions(null);
            assertTrue(result.isEmpty());
            verifyNoInteractions(permissionGateway);
        }

        @Test
        @DisplayName("admin 角色用户返回通配符权限")
        void adminUser_returnsWildcard() {
            when(permissionGateway.isAdmin(1L)).thenReturn(true);

            Set<String> result = permissionService.getUserPermissions(1L);

            assertEquals(Set.of("*:*:*"), result);
            verify(permissionGateway, never()).collectRolePermissions(anyLong());
        }

        @Test
        @DisplayName("普通用户返回合并后的权限集合")
        void normalUser_returnsMergedPerms() {
            when(permissionGateway.isAdmin(2L)).thenReturn(false);
            when(permissionGateway.collectRolePermissions(2L))
                    .thenReturn(Set.of("system:user:list", "system:role:list"));

            Set<String> result = permissionService.getUserPermissions(2L);

            assertTrue(result.contains("system:user:list"));
            assertTrue(result.contains("system:role:list"));
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("hasPermission()")
    class HasPermission {

        @Test
        @DisplayName("null 参数返回 false")
        void nullParams_returnsFalse() {
            assertFalse(permissionService.hasPermission(null, "system:user:list"));
            assertFalse(permissionService.hasPermission(1L, null));
        }

        @Test
        @DisplayName("admin 用户拥有所有权限")
        void adminUser_hasAllPermissions() {
            when(permissionGateway.isAdmin(1L)).thenReturn(true);

            assertTrue(permissionService.hasPermission(1L, "any:perm"));
            assertTrue(permissionService.hasPermission(1L, "system:user:delete"));
        }

        @Test
        @DisplayName("普通用户检查具体权限")
        void normalUser_checksSpecificPerm() {
            when(permissionGateway.isAdmin(2L)).thenReturn(false);
            when(permissionGateway.collectRolePermissions(2L))
                    .thenReturn(Set.of("system:user:list"));

            assertTrue(permissionService.hasPermission(2L, "system:user:list"));
            assertFalse(permissionService.hasPermission(2L, "system:user:delete"));
        }
    }

    @Nested
    @DisplayName("hasAllPermissions()")
    class HasAllPermissions {

        @Test
        @DisplayName("null 或空数组返回 false")
        void invalidInput_returnsFalse() {
            assertFalse(permissionService.hasAllPermissions(1L, null));
            assertFalse(permissionService.hasAllPermissions(null, new String[]{"a"}));
            assertFalse(permissionService.hasAllPermissions(1L, new String[]{}));
        }

        @Test
        @DisplayName("admin 用户拥有所有权限组合")
        void adminUser_hasAllPerms() {
            when(permissionGateway.isAdmin(1L)).thenReturn(true);

            assertTrue(permissionService.hasAllPermissions(1L, new String[]{"a:b:c", "d:e:f"}));
        }

        @Test
        @DisplayName("普通用户缺一不可")
        void normalUser_missingOnePerm_fails() {
            when(permissionGateway.isAdmin(3L)).thenReturn(false);
            when(permissionGateway.collectRolePermissions(3L))
                    .thenReturn(Set.of("perm:a", "perm:b"));

            assertFalse(permissionService.hasAllPermissions(3L, new String[]{"perm:a", "perm:c"}));
        }
    }

    @Test
    @DisplayName("evictUserPermissions() 调用缓存清除")
    void evictUserPermissions() {
        permissionService.evictUserPermissions(1L);
        // 验证方法可正常调用（无返回值，无副作用验证）
    }
}