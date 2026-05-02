package com.linan.core.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 动态数据源切换 AOP 切面
 *
 * <p>拦截所有标注了 {@link Ds} 注解的方法或类，在方法执行前设置数据源上下文，
 * 方法执行后清理上下文。</p>
 *
 * <p>优先级为 {@code @Order(-1)}，高于 Spring 事务拦截器的默认优先级，
 * 确保在事务开启前完成数据源切换，避免事务使用错误的数据源。</p>
 *
 * <p>支持注解在类级别和方法级别：</p>
 * <ul>
 *   <li>类级别：该类中所有方法默认使用指定的数据源</li>
 *   <li>方法级别：该方法覆盖类级别的数据源设置</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@Order(-1)
public class DsAspect {

    /**
     * 环绕通知：拦截 {@code @Ds} 注解
     *
     * <p>匹配规则：</p>
     * <ul>
     *   <li>{@code @annotation(ds)} — 匹配方法上的 {@code @Ds} 注解</li>
     *   <li>{@code @within(ds)} — 匹配类上的 {@code @Ds} 注解（该类中所有方法）</li>
     * </ul>
     *
     * @param joinPoint 连接点
     * @param ds        {@link Ds} 注解实例
     * @return 方法执行结果
     * @throws Throwable 方法执行过程中抛出的异常
     */
    @Around("@annotation(ds) || @within(ds)")
    public Object around(ProceedingJoinPoint joinPoint, Ds ds) throws Throwable {
        // 获取实际生效的 Ds 注解
        // 如果方法上有 @Ds 注解，优先使用方法上的；否则使用类上的
        Ds resolvedDs = resolveDsAnnotation(joinPoint, ds);
        String dataSourceName = resolvedDs.value();

        if (log.isDebugEnabled()) {
            log.debug("Switch datasource to [{}] in method [{}#{}]",
                    dataSourceName,
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName());
        }

        // 设置数据源上下文
        DsContextHolder.set(dataSourceName);

        try {
            // 执行原方法
            return joinPoint.proceed();
        } finally {
            // 清理数据源上下文（防止 ThreadLocal 内存泄漏）
            DsContextHolder.clear();
            if (log.isDebugEnabled()) {
                log.debug("Clear datasource context after method [{}#{}]",
                        joinPoint.getTarget().getClass().getSimpleName(),
                        joinPoint.getSignature().getName());
            }
        }
    }

    /**
     * 解析实际生效的 {@link Ds} 注解
     *
     * <p>当方法上和类上同时有 {@code @Ds} 注解时，方法级别的注解优先。</p>
     *
     * @param joinPoint  连接点
     * @param ds         {@code @within(ds)} 匹配到的类级别注解（可能为 null）
     * @return 实际生效的 {@link Ds} 注解
     */
    private Ds resolveDsAnnotation(ProceedingJoinPoint joinPoint, Ds ds) {
        // 先尝试获取方法上的 @Ds 注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Ds methodDs = method.getAnnotation(Ds.class);

        if (methodDs != null) {
            return methodDs;
        }

        // 如果方法上没有，则使用类上的（由 @within 匹配到的）
        if (ds != null) {
            return ds;
        }

        // 这里实际上不会执行到，因为切点表达式确保了一定有 @Ds 注解
        // 但为了安全，返回一个默认值
        return method.getDeclaringClass().getAnnotation(Ds.class);
    }
}
