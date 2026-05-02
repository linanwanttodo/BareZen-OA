package com.linan.core.security;

import java.lang.annotation.*;

/**
 * 数据权限注解
 *
 * <p>标注在 Mapper 接口方法上（或 Mapper 接口类上），由
 * {@link DataScopeInterceptor} 根据 MappedStatement 的 ID 反射查找并生效。
 * 当方法标注该注解后，SELECT 查询会自动追加当前用户的数据范围过滤条件。</p>
 *
 * <p>数据权限范围（定义在 sys_role.data_scope）：</p>
 * <ul>
 *   <li>1：仅本人数据</li>
 *   <li>2：本部门数据</li>
 *   <li>3：本部门及子部门数据</li>
 *   <li>4：全部数据（超级管理员）</li>
 * </ul>
 *
 * <p>使用方式（Mapper 接口方法）：</p>
 * <pre>{@code
 * @DataScope(deptAlias = "d", userAlias = "u")
 * List<SysUser> selectUsersByRoleDeptPage(...);
 * }</pre>
 *
 * @author BareZen Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 部门表的别名（在 Mapper XML 中定义的表别名）
     * 例如：SELECT * FROM sys_dept d -> deptAlias = "d"
     */
    String deptAlias() default "d";

    /**
     * 用户表的别名（在 Mapper XML 中定义的表别名）
     * 例如：SELECT * FROM sys_user u -> userAlias = "u"
     */
    String userAlias() default "u";
}
