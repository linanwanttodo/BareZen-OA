package com.linan.core.quartz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 调度配置
 *
 * 当 classpath 中存在 org.quartz.Scheduler 类时自动启用。
 * Spring Boot 的 spring-boot-starter-quartz 已提供自动配置，
 * 无需额外声明 SchedulerFactoryBean 等 Bean。
 *
 * 如需要自定义 Quartz 属性，可在 application.yml 中配置 spring.quartz.*。
 *
 * @author BareZen Team
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.quartz.Scheduler")
public class ScheduleConfig {

    /**
     * 配置类初始化日志
     *
     * 仅在 Quartz 可用时打印，方便确认调度框架已加载。
     */
    public ScheduleConfig() {
        log.info("Quartz 调度框架已加载");
    }
}
