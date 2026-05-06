package com.linan.core.security;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.core.spi.permission.PermissionGateway;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

import org.apache.ibatis.mapping.ParameterMapping;

/**
 * 数据权限拦截器
 *
 * <p>基于 MyBatis-Plus 的 InnerInterceptor，在执行 SELECT 查询前，
 * 根据当前用户的数据权限范围，自动向 SQL 追加部门/用户过滤条件。</p>
 *
 * <p>拦截原理：</p>
 * <ol>
 *   <li>获取当前线程上的 MappedStatement</li>
 *   <li>检查该方法或类是否标注了 @DataScope 注解</li>
 *   <li>获取当前用户的角色和对应的数据权限范围</li>
 *   <li>使用 JSqlParser 解析 SQL，追加 WHERE 条件</li>
 * </ol>
 *
 * <p>数据权限范围说明：</p>
 * <ul>
 *   <li>data_scope = 1：仅本人数据（WHERE u.create_by = 当前用户ID）</li>
 *   <li>data_scope = 2：本部门数据（WHERE d.dept_id IN 用户所在部门）</li>
 *   <li>data_scope = 3：本部门及子部门（递归查询子部门）</li>
 *   <li>data_scope = 4：全部数据（不追加条件）</li>
 * </ul>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScopeInterceptor implements InnerInterceptor {

    private final PermissionGateway permissionGateway;

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        // 仅处理 SELECT 语句
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return;
        }

        // 获取当前用户
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return;
        }

        // 获取方法上的 @DataScope 注解（从 MappedStatement 的 ID 推断）
        DataScope dataScope = getDataScopeAnnotation(ms);
        if (dataScope == null) {
            return;
        }

        try {
            // 超管不限制数据权限
            if (permissionGateway.isAdmin(userId)) {
                return;
            }

            // 所有角色数据权限取最严格范围
            int minScope = permissionGateway.minDataScope(userId);

            // 构建过滤条件（使用参数化占位符替代字面量拼接）
            String whereSql = buildParametrizedWhereSql(minScope, userId, dataScope, boundSql, ms);
            if (whereSql == null) {
                return;
            }

            // 使用 JSqlParser 解析并追加 WHERE 条件
            String originalSql = boundSql.getSql();
            Select select = (Select) CCJSqlParserUtil.parse(originalSql);
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

            Expression whereExpression = plainSelect.getWhere();
            Expression additionalWhere = CCJSqlParserUtil.parseExpression(whereSql);

            if (whereExpression == null) {
                plainSelect.setWhere(additionalWhere);
            } else {
                plainSelect.setWhere(new AndExpression(whereExpression, additionalWhere));
            }

            // 使用 MyBatis-Plus MetaObject API 修改 BoundSql 中的 SQL（替代反射方式，兼容 JDK 17+）
            Configuration configuration = ms.getConfiguration();
            org.apache.ibatis.reflection.MetaObject metaObject = configuration.newMetaObject(boundSql);
            metaObject.setValue("sql", select.toString());

        } catch (JSQLParserException e) {
            log.warn("数据权限 SQL 解析失败: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("数据权限处理异常: {}", e.getMessage());
        }
    }

    /**
     * 根据数据权限范围构建参数化的 WHERE 条件 SQL
     *
     * <p>使用 JDBC {@code ?} 占位符和 {@link ParameterMapping} 替代直接拼接字面量，
     * 避免 SQL 注入风险。参数值通过 {@link BoundSql#setAdditionalParameter} 注册。</p>
     */
    private String buildParametrizedWhereSql(int dataScope, Long userId, DataScope annotation,
                                              BoundSql boundSql, MappedStatement ms) {
        String deptAlias = annotation.deptAlias();
        String userAlias = annotation.userAlias();

        switch (dataScope) {
            case 1: { // 仅本人数据
                String paramName = "datascope_userId";
                addParameterMapping(boundSql, ms, Long.class, paramName);
                boundSql.setAdditionalParameter(paramName, userId);
                return " " + userAlias + ".create_by = ? ";
            }
            case 2: { // 本部门数据
                List<Long> deptIds = permissionGateway.selectDeptIdsByUserId(userId);
                if (deptIds.isEmpty()) {
                    return " 1=0 "; // 无部门则查不到数据
                }
                StringBuilder sb = new StringBuilder(" " + deptAlias + ".dept_id IN (");
                for (int i = 0; i < deptIds.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append("?");
                    String paramName = "datascope_dept_" + i;
                    addParameterMapping(boundSql, ms, Long.class, paramName);
                    boundSql.setAdditionalParameter(paramName, deptIds.get(i));
                }
                sb.append(") ");
                return sb.toString();
            }
            case 3: { // 本部门及子部门数据
                List<Long> allDeptIds = getAllDeptIds(userId);
                if (allDeptIds.isEmpty()) {
                    return " 1=0 ";
                }
                StringBuilder sb = new StringBuilder(" " + deptAlias + ".dept_id IN (");
                for (int i = 0; i < allDeptIds.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append("?");
                    String paramName = "datascope_alldept_" + i;
                    addParameterMapping(boundSql, ms, Long.class, paramName);
                    boundSql.setAdditionalParameter(paramName, allDeptIds.get(i));
                }
                sb.append(") ");
                return sb.toString();
            }
            case 4: // 全部数据
            default:
                return null; // 不追加条件
        }
    }

    /**
     * 向 BoundSql 注册参数映射，使 JDBC {@code ?} 占位符能够正确绑定参数值。
     *
     * @param boundSql  当前 SQL 的参数上下文
     * @param ms        当前 MappedStatement，用于获取 Configuration
     * @param paramType 参数类型
     * @param paramName 参数名称（用于 {@link BoundSql#setAdditionalParameter}）
     */
    private void addParameterMapping(BoundSql boundSql, MappedStatement ms,
                                      Class<?> paramType, String paramName) {
        ParameterMapping mapping = new ParameterMapping.Builder(
                ms.getConfiguration(), paramName, paramType).build();
        boundSql.getParameterMappings().add(mapping);
    }

    /**
     * 递归获取用户所在部门及其所有子部门 ID
     */
    private List<Long> getAllDeptIds(Long userId) {
        List<Long> userDeptIds = permissionGateway.selectDeptIdsByUserId(userId);
        Set<Long> result = new HashSet<>(userDeptIds);

        for (Long deptId : userDeptIds) {
            collectChildDeptIds(deptId, result);
        }
        return new ArrayList<>(result);
    }

    /**
     * 递归收集子部门 ID
     */
    private void collectChildDeptIds(Long parentId, Set<Long> result) {
        List<Long> children = permissionGateway.selectChildDeptIds(parentId);
        for (Long childId : children) {
            if (result.add(childId)) {
                collectChildDeptIds(childId, result);
            }
        }
    }

    /**
     * 从 MappedStatement 获取 @DataScope 注解
     */
    private DataScope getDataScopeAnnotation(MappedStatement ms) {
        try {
            String id = ms.getId();
            // id 格式：com.linan.system.mapper.SysUserMapper.selectUsersByRoleDeptPage
            String className = id.substring(0, id.lastIndexOf('.'));
            String methodName = id.substring(id.lastIndexOf('.') + 1);

            Class<?> clazz = Class.forName(className);

            // 先找方法级别的注解
            for (java.lang.reflect.Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(DataScope.class)) {
                    return method.getAnnotation(DataScope.class);
                }
            }

            // 再找类级别的注解
            if (clazz.isAnnotationPresent(DataScope.class)) {
                return clazz.getAnnotation(DataScope.class);
            }
        } catch (ClassNotFoundException e) {
            log.debug("DataScope: 无法加载类 {}", e.getMessage());
        }
        return null;
    }
}
