package com.linan.system.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SysMenuMapper 菜单查询状态语义契约测试（W30）
 *
 * <p>项目统一 sys_menu.status 语义为 '0'=正常、'1'=停用（与前端 format.ts、
 * 菜单管理页及 V48 归一化迁移一致）。历史版本曾把用户菜单查询写成
 * {@code m.status = '1'}，归一化后会导致正常菜单被过滤、用户侧路由为空（P0 回归）。
 * 本测试通过反射读取 {@code @Select} SQL，锁定查询必须按启用值 '0' 过滤，
 * 防止后续再被误改回 '1'。</p>
 *
 * @author BareZen Team
 */
class SysMenuMapperStatusContractTest {

    private String selectSqlOf(String methodName) throws NoSuchMethodException {
        Method method = Arrays.stream(SysMenuMapper.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(methodName));
        Select select = method.getAnnotation(Select.class);
        assertNotNull(select, methodName + " 必须使用 @Select 注解");
        return String.join(" ", select.value());
    }

    @Test
    @DisplayName("selectMenusByUserId 按启用状态 '0' 过滤菜单")
    void selectMenusByUserId_filtersEnabledStatus() throws Exception {
        String sql = selectSqlOf("selectMenusByUserId");
        assertTrue(sql.contains("m.status = '0'"),
                "用户菜单查询必须按启用值 status='0' 过滤，实际 SQL: " + sql);
    }

    @Test
    @DisplayName("selectMenusByUserId 不得残留停用值 '1' 过滤条件")
    void selectMenusByUserId_doesNotFilterDisabledStatus() throws Exception {
        String sql = selectSqlOf("selectMenusByUserId");
        assertTrue(!sql.contains("m.status = '1'"),
                "用户菜单查询不得按停用值 status='1' 过滤，实际 SQL: " + sql);
    }
}
