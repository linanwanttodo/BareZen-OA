package com.linan.core.quartz;

import com.linan.core.common.util.SpringContextHolder;
import com.linan.system.entity.SysJobLog;
import com.linan.system.mapper.SysJobLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Quartz 任务调度执行器
 *
 * <p>实现 org.quartz.Job 接口，作为所有 Quartz 定时任务的统一执行入口。
 * 从 JobDataMap 中读取 "invokeTarget" 参数，解析出 beanName 和 methodName，
 * 通过 SpringContextHolder 获取 Spring Bean 并反射调用目标方法。</p>
 *
 * <p>执行完成后自动记录 {@link SysJobLog} 到数据库。</p>
 *
 * <p>invokeTarget 格式：beanName.methodName（例如：cleanTask.cleanOperLog）</p>
 *
 * <h3>安全机制</h3>
 * <ul>
 *   <li>Bean 名称白名单：仅允许以白名单前缀开头的 bean 被调用</li>
 *   <li>危险类黑名单：禁止调用 {@code java.lang.Runtime}、{@code java.lang.ProcessBuilder} 等危险类的方法</li>
 * </ul>
 *
 * @author BareZen Team
 */
@Slf4j
public class ScheduleRunnable implements Job {

    /**
     * 允许调用的 Bean 名称前缀白名单。
     * 只有以这些前缀开头的 bean 才允许通过定时任务反射调用。
     */
    private static final List<String> ALLOWED_BEAN_PREFIXES = Arrays.asList(
            "clean", "sync", "task", "job", "backup", "report", "stat"
    );

    /**
     * 禁止反射调用的危险类（全限定类名）。
     * 这些类的实例即使被注册为 Spring Bean，也不允许通过定时任务调用。
     */
    private static final List<String> DANGEROUS_CLASSES = Arrays.asList(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.ProcessImpl",
            "java.lang.Process",
            "java.lang.ProcessHandle",
            "java.lang.System",
            "java.lang.Shutdown",
            "java.io.FilePermission",
            "java.security.AccessController",
            "java.security.AccessControlContext"
    );

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String invokeTarget = context.getMergedJobDataMap().getString("invokeTarget");
        if (invokeTarget == null || invokeTarget.isBlank()) {
            log.warn("定时任务执行失败: invokeTarget 为空");
            return;
        }

        long startTime = System.currentTimeMillis();
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobId((long) (context.getJobDetail().getKey().hashCode() & 0x7fffffff));
        jobLog.setJobName(context.getJobDetail().getKey().getName());
        jobLog.setJobGroup(context.getJobDetail().getKey().getGroup());
        jobLog.setInvokeTarget(invokeTarget);
        jobLog.setCronExpression(context.getTrigger().getDescription());
        jobLog.setStartTime(LocalDateTime.now());

        try {
            // 解析 "beanName.methodName"
            String[] parts = invokeTarget.split("\\.");
            if (parts.length < 2) {
                throw new JobExecutionException("invokeTarget 格式错误: " + invokeTarget);
            }

            String beanName = parts[0];
            String methodName = parts[1];

            // === 安全校验：Bean 名称白名单 ===
            boolean allowed = ALLOWED_BEAN_PREFIXES.stream()
                    .anyMatch(prefix -> beanName.startsWith(prefix));
            if (!allowed) {
                String msg = "定时任务执行被拒绝: Bean 名称 '" + beanName
                        + "' 不在白名单中，允许的前缀: " + ALLOWED_BEAN_PREFIXES;
                log.warn(msg);
                jobLog.setStatus("1");
                jobLog.setErrorMsg(msg);
                jobLog.setStopTime(LocalDateTime.now());
                jobLog.setDuration(System.currentTimeMillis() - startTime);
                saveJobLog(jobLog);
                return;
            }

            // 从 Spring 容器中获取 Bean
            Object bean = SpringContextHolder.getBean(beanName);

            // === 安全校验：危险类黑名单 ===
            String className = bean.getClass().getName();
            // 解析实际的目标类名（处理 AOP 代理场景，如 MyBatis MapperProxy、CGLIB 代理等）
            String resolvedClassName = className;
            Class<?> targetClass = bean.getClass();
            while (targetClass.isSynthetic() || java.lang.reflect.Proxy.isProxyClass(targetClass)
                    || targetClass.getName().contains("$$")) {
                Class<?>[] interfaces = targetClass.getInterfaces();
                if (interfaces.length > 0) {
                    targetClass = interfaces[0];
                    break;
                }
                targetClass = targetClass.getSuperclass();
            }
            if (targetClass != bean.getClass()) {
                resolvedClassName = targetClass.getName();
            }
            // 用解析后的类名匹配黑名单（使用最终局部变量以支持 Lambda 捕获）
            String checkClassName = resolvedClassName;
            boolean isDangerous = DANGEROUS_CLASSES.stream()
                    .anyMatch(dc -> checkClassName.equals(dc) || checkClassName.startsWith(dc + "$"));
            if (isDangerous) {
                String msg = "定时任务执行被拒绝: 禁止调用危险类 '" + className + "' 的方法";
                log.warn(msg);
                jobLog.setStatus("1");
                jobLog.setErrorMsg(msg);
                jobLog.setStopTime(LocalDateTime.now());
                jobLog.setDuration(System.currentTimeMillis() - startTime);
                saveJobLog(jobLog);
                return;
            }

            // 反射调用方法
            Method method = bean.getClass().getMethod(methodName);
            method.invoke(bean);

            jobLog.setStatus("0");
            log.info("定时任务执行成功: invokeTarget={}", invokeTarget);
        } catch (NoSuchMethodException e) {
            jobLog.setStatus("1");
            jobLog.setErrorMsg("方法不存在: " + invokeTarget);
            log.error("定时任务执行失败: 方法不存在, invokeTarget={}", invokeTarget, e);
        } catch (JobExecutionException e) {
            // 已经是包装过的异常，直接设置
            jobLog.setStatus("1");
            jobLog.setErrorMsg(e.getMessage());
            log.error("定时任务执行失败: invokeTarget={}", invokeTarget, e);
        } catch (Exception e) {
            jobLog.setStatus("1");
            jobLog.setErrorMsg(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            log.error("定时任务执行失败: invokeTarget={}", invokeTarget, e);
        } finally {
            jobLog.setStopTime(LocalDateTime.now());
            jobLog.setDuration(System.currentTimeMillis() - startTime);
            saveJobLog(jobLog);
        }
    }

    /**
     * 异步保存任务执行日志
     */
    private void saveJobLog(SysJobLog jobLog) {
        try {
            SysJobLogMapper mapper = SpringContextHolder.getBean(SysJobLogMapper.class);
            mapper.insert(jobLog);
        } catch (Exception e) {
            log.warn("保存任务执行日志失败", e);
        }
    }
}
