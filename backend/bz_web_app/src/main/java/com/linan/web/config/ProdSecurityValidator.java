package com.linan.web.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 生产环境启动校验（fail-fast）
 *
 * <p>仅 {@code prod} profile 加载。校验 JWT 密钥：为空或仍为 dev 占位密钥时
 * 直接抛出 {@link IllegalStateException} 终止应用启动——dev 占位密钥长度合规
 * （可通过 JwtUtil 的密钥长度校验），若被误用于生产即签名密钥公开，因此
 * 在此显式拦截。</p>
 *
 * <p>生产部署必须通过环境变量 {@code JWT_SECRET} 注入随机密钥
 * （{@code openssl rand -base64 32}），application-prod.yml 中
 * {@code jwt.secret: ${JWT_SECRET}} 无默认值。</p>
 */
@Slf4j
@Configuration
@Profile("prod")
public class ProdSecurityValidator {

    /**
     * 与 application-dev.yml 中的 dev 占位密钥一致
     * （dev-only-DO-NOT-USE-IN-PROD-replace-with-openssl-rand-base64-32）
     */
    static final String DEV_PLACEHOLDER_SECRET =
            "dev-only-DO-NOT-USE-IN-PROD-replace-with-openssl-rand-base64-32";

    private final String jwtSecret;

    public ProdSecurityValidator(@Value("${jwt.secret:}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    void init() {
        validate();
    }

    /**
     * 校验 JWT 密钥：非空且不等于 dev 占位值，否则抛异常终止启动。
     *
     * <p>包可见，便于单测直接调用（不依赖 Spring 生命周期）。</p>
     */
    void validate() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "生产环境 JWT 密钥未配置：请设置环境变量 JWT_SECRET（建议 openssl rand -base64 32）");
        }
        if (DEV_PLACEHOLDER_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "生产环境禁止使用 dev 占位 JWT 密钥，请通过 JWT_SECRET 注入随机密钥后重启");
        }
        log.info("JWT 密钥校验通过（prod）");
    }
}