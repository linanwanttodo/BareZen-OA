package com.linan.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BareZen Base 后端应用主启动类
 *
 * 这是整个 Spring Boot 应用的入口点。使用 @SpringBootApplication 注解
 * 自动启用组件扫描、自动配置和属性支持。
 *
 * <p><b>组件扫描说明</b>：</p>
 * 由于主启动类位于 com.linan.web 包中，Spring Boot 默认只扫描当前包及其子包。
 * 核心底座模块（bz_framework_core）的代码位于 com.linan.core 包中，
 * 需要显式指定 scanBasePackages 参数来告知 Spring 扫描两个模块的包。
 *
 * <p><b>启动方式</b>：</p>
 * <pre>
 * # 命令行（在 backend 目录下）：
 * ./gradlew :bz_web_app:bootRun
 *
 * # 或使用 Makefile：
 * make dev-api
 *
 * # 或在 IDEA 中直接运行此类的 main 方法
 * </pre>
 *
 * @author Zen-Base Team
 * @since 1.0-SNAPSHOT
 */
@SpringBootApplication(scanBasePackages = {
        "com.linan.core",
        "com.linan.system",
        "com.linan.system.monitor",
        "com.linan.hr",
        "com.linan.asset",
        "com.linan.office",
        "com.linan.kb",
        "com.linan.web"
})
@EnableAsync
@EnableCaching
@EnableScheduling
public class BareZenApplication {

    /**
     * 应用启动入口
     *
     * SpringApplication.run() 负责：
     * 1. 创建 Spring 应用上下文（ApplicationContext）
     * 2. 执行自动配置（AutoConfiguration）
     * 3. 启动内嵌的 Tomcat 服务器（默认端口 8080）
     * 4. 注册所有 Bean（Controller、Service、拦截器等）
     * 5. 完成应用启动
     *
     * @param args 命令行参数（可以通过 --server.port=9090 等方式覆盖配置）
     */
    public static void main(String[] args) {
        SpringApplication.run(BareZenApplication.class, args);
    }
}