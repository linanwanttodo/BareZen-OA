package com.linan.core.spi.permission;

import java.util.List;
import java.util.Set;

/**
 * 权限/数据权限查询 SPI（core 层契约，由系统模块注入实现）
 *
 * <p>反转 core → system 的依赖方向：core 只依赖本接口的语义契约，
 * 具体查询落到 com.linan.system 的 mapper/实体上，由 system 侧适配器实现。</p>
 *
 * @author BareZen Team
 */
public interface PermissionGateway {

    /** 用户是否超级管理员（持有角色键 admin） */
    boolean isAdmin(Long userId);

    /** 收集用户所有角色的权限标识并集（不含超管通配符） */
    Set<String> collectRolePermissions(Long userId);

    /** 用户角色最小数据权限范围（最严格者，无则默认 4=全部） */
    int minDataScope(Long userId);

    /** 用户直接所属的部门 ID 列表 */
    List<Long> selectDeptIdsByUserId(Long userId);

    /** 指定部门的下级子部门 ID 列表 */
    List<Long> selectChildDeptIds(Long parentId);
}