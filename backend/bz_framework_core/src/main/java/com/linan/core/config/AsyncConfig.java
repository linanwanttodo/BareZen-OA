package com.linan.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 *
 * <p>{@code @Async} 若未指定执行器，Spring 默认使用 {@code SimpleAsyncTaskExecutor}——
 * 该执行器每次任务都新建线程且无队列上限，高并发异步通知（操作日志、公告广播、
 * 审批推送）下会无界创建线程耗尽资源。此处显式配置有界线程池，由
 * {@code @Async} 自动选用（实现 {@link AsyncConfigurer}）。</p>
 *
 * <ul>
 *   <li>core=4 / max=8：通知类任务短小高频，无需过大核心线程</li>
 *   <li>queue=200：队列积压上限，避免无界堆积</li>
 *   <li>CallerRuns 拒绝策略：队列满时由调用线程执行，保证通知不丢失且不抛异常</li>
 *   <li>setWaitForTasksToCompleteOnShutdown + 优雅停机</li>
 * </ul>
 *
 * @author BareZen Team
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");
        // 队列满时由调用线程执行，避免异步通知在极端流量下丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 供需要显式注入线程池的组件使用（如手动 submit 的异步场景）
     */
    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        return getAsyncExecutor();
    }
}
