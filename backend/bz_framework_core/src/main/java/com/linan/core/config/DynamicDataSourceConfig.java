package com.linan.core.config;

import com.linan.core.datasource.DynamicDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 动态数据源配置
 *
 * <p>读取 application.yml 中的数据源配置，创建主从数据源并注册到 {@link DynamicDataSource} 中。</p>
 *
 * <p>主数据源配置项（必填）：</p>
 * <ul>
 *   <li>{@code spring.datasource.url}</li>
 *   <li>{@code spring.datasource.username}</li>
 *   <li>{@code spring.datasource.password}</li>
 *   <li>{@code spring.datasource.driver-class-name}</li>
 * </ul>
 *
 * <p>从数据源配置项（可选）：</p>
 * <ul>
 *   <li>{@code spring.datasource.slave.url}</li>
 *   <li>{@code spring.datasource.slave.username}</li>
 *   <li>{@code spring.datasource.slave.password}</li>
 *   <li>{@code spring.datasource.slave.driver-class-name}</li>
 * </ul>
 *
 * <p>如果未配置从数据源，系统仍可正常运行，所有请求将使用主数据源。</p>
 *
 * <p>此配置会覆盖 Spring Boot 默认的 DataSource 自动配置（通过 {@code @Primary}），
 * 确保所有数据库操作都通过 {@link DynamicDataSource} 进行路由。</p>
 */
@Slf4j
@Configuration
public class DynamicDataSourceConfig {

    /**
     * 主数据源在 targetDataSources 中的 key
     */
    private static final String MASTER_KEY = "master";

    /**
     * 从数据源在 targetDataSources 中的 key
     */
    private static final String SLAVE_KEY = "slave";

    /**
     * 创建动态数据源
     *
     * <p>读取配置并创建主/从 HikariCP 连接池，包装为 {@link DynamicDataSource}。
     * 使用 {@code @Primary} 确保此 DataSource 被注入到所有需要 DataSource 的地方。</p>
     *
     * @param env Spring Environment，用于读取配置属性
     * @return 动态数据源实例
     */
    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();

        // ---- 创建主数据源 ----
        HikariDataSource master = createDataSource(env, "spring.datasource");
        dynamicDataSource.setDefaultTargetDataSource(master);

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(MASTER_KEY, master);

        // ---- 可选：创建从数据源 ----
        String slaveUrl = env.getProperty("spring.datasource.slave.url");
        if (StringUtils.hasText(slaveUrl)) {
            HikariDataSource slave = createDataSource(env, "spring.datasource.slave");
            targetDataSources.put(SLAVE_KEY, slave);
            log.info("Dynamic data source configured: master [{}], slave [{}]",
                    env.getProperty("spring.datasource.url"),
                    slaveUrl);
        } else {
            log.info("Dynamic data source configured: master only [{}], no slave datasource configured",
                    env.getProperty("spring.datasource.url"));
        }

        dynamicDataSource.setTargetDataSources(targetDataSources);
        return dynamicDataSource;
    }

    /**
     * 根据配置前缀创建 HikariCP 数据源
     *
     * <p>读取的属性前缀示例：{@code spring.datasource} 或 {@code spring.datasource.slave}。</p>
     *
     * <p>支持的配置项：</p>
     * <ul>
     *   <li>{prefix}.url — JDBC 连接 URL（必填）</li>
     *   <li>{prefix}.username — 数据库用户名（必填）</li>
     *   <li>{prefix}.password — 数据库密码（必填）</li>
     *   <li>{prefix}.driver-class-name — 驱动类名（可选，HikariCP 可自动识别）</li>
     *   <li>{prefix}.hikari.minimum-idle — 最小空闲连接数</li>
     *   <li>{prefix}.hikari.maximum-pool-size — 最大连接数</li>
     *   <li>{prefix}.hikari.connection-timeout — 连接超时时间（毫秒）</li>
     *   <li>{prefix}.hikari.idle-timeout — 空闲连接超时（毫秒）</li>
     *   <li>{prefix}.hikari.max-lifetime — 连接最大存活时间（毫秒）</li>
     * </ul>
     *
     * @param env    Spring Environment
     * @param prefix 配置前缀
     * @return HikariCP 数据源实例
     */
    private HikariDataSource createDataSource(Environment env, String prefix) {
        HikariConfig config = new HikariConfig();

        // ---- 核心连接配置 ----
        String url = env.getProperty(prefix + ".url");
        String username = env.getProperty(prefix + ".username");
        String password = env.getProperty(prefix + ".password");
        String driverClassName = env.getProperty(prefix + ".driver-class-name");

        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("DataSource URL is not configured: " + prefix + ".url");
        }

        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        if (StringUtils.hasText(driverClassName)) {
            config.setDriverClassName(driverClassName);
        }

        // ---- HikariCP 连接池配置（可选） ----
        Integer minimumIdle = getProperty(env, prefix + ".hikari.minimum-idle", Integer.class);
        if (minimumIdle != null) {
            config.setMinimumIdle(minimumIdle);
        }

        Integer maximumPoolSize = getProperty(env, prefix + ".hikari.maximum-pool-size", Integer.class);
        if (maximumPoolSize != null) {
            config.setMaximumPoolSize(maximumPoolSize);
        }

        Long connectionTimeout = getProperty(env, prefix + ".hikari.connection-timeout", Long.class);
        if (connectionTimeout != null) {
            config.setConnectionTimeout(connectionTimeout);
        }

        Long idleTimeout = getProperty(env, prefix + ".hikari.idle-timeout", Long.class);
        if (idleTimeout != null) {
            config.setIdleTimeout(idleTimeout);
        }

        Long maxLifetime = getProperty(env, prefix + ".hikari.max-lifetime", Long.class);
        if (maxLifetime != null) {
            config.setMaxLifetime(maxLifetime);
        }

        // ---- 连接池命名（便于监控） ----
        String poolName = MASTER_KEY.equals(determineDsKey(prefix)) ? "HikariPool-Master" : "HikariPool-Slave";
        config.setPoolName(poolName);

        return new HikariDataSource(config);
    }

    /**
     * 从 Environment 中读取指定类型的属性
     *
     * @param env   Spring Environment
     * @param key   属性键
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 属性值，如果未配置则返回 null
     */
    private <T> T getProperty(Environment env, String key, Class<T> clazz) {
        try {
            return env.getProperty(key, clazz);
        } catch (Exception e) {
            log.debug("Failed to read property [{}]: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 根据配置前缀判断数据源 key
     */
    private String determineDsKey(String prefix) {
        if (prefix.contains("slave")) {
            return SLAVE_KEY;
        }
        return MASTER_KEY;
    }
}
