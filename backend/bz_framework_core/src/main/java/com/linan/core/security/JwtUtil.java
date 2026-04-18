package com.linan.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 *
 * <p>负责 JWT Token 的签发、解析和摘要计算。
 * 密钥从 application.yml 的 {@code jwt.secret} 读取，
 * 生产环境务必通过环境变量 {@code JWT_SECRET} 注入，禁止在配置文件中硬编码。</p>
 *
 * <h3>安全要求</h3>
 * <ul>
 *   <li>密钥长度必须 ≥ 32 字符（HMAC-SHA256 要求 256 位密钥）</li>
 *   <li>生产环境使用 {@code openssl rand -base64 32} 生成随机密钥</li>
 *   <li>不同环境（开发/测试/生产）必须使用不同的密钥</li>
 * </ul>
 *
 * <h3>启动验证</h3>
 * 构造函数执行后，{@link #validateKey()} 方法会自动校验密钥强度，
 * 弱密钥或缺失密钥会抛出 {@link IllegalStateException}，阻止应用启动。
 */
@Slf4j
@Component
public class JwtUtil {

    /** 密钥（从配置读取，支持环境变量覆盖） */
    private final String secret;

    /** HMAC 密钥对象 */
    private final SecretKey key;

    /** Token 过期时间（毫秒），默认 24 小时 */
    private final long expiration;

    /**
     * HMAC-SHA256 要求的最小密钥长度（256 位 = 32 字节）
     * 见：https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html#mac-algorithms
     */
    private static final int MIN_SECRET_LENGTH = 32;

    /**
     * 已知的弱密钥列表（用于启动时告警）
     * 这些密钥来自示例代码、教程文档或开源仓库，不应在生产环境中使用。
     */
    private static final String[] KNOWN_WEAK_SECRETS = {
            "BareZenBaseSecretKeyForJWTSigning2024"
    };

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:86400000}") long expiration) {
        this.secret = secret;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 启动时校验密钥强度。
     *
     * <p>如果密钥不满足安全要求，直接抛出异常阻止应用启动（fail-fast 原则），
     * 避免在弱密钥下运行导致安全风险。</p>
     */
    @PostConstruct
    public void validateKey() {
        if (secret == null || secret.isEmpty()) {
            log.error("JWT 密钥未配置！请在 application.yml 中设置 jwt.secret，"
                    + "或通过环境变量 JWT_SECRET 注入。"
                    + "生成安全密钥：openssl rand -base64 32");
            throw new IllegalStateException("JWT secret is not configured");
        }

        if (secret.length() < MIN_SECRET_LENGTH) {
            log.error("JWT 密钥长度不足：当前 {} 字符，至少需要 {} 字符（256位）。"
                            + "请使用更长的密钥，例如：openssl rand -base64 32",
                    secret.length(), MIN_SECRET_LENGTH);
            throw new IllegalStateException(
                    "JWT secret too short: " + secret.length()
                            + " chars, minimum " + MIN_SECRET_LENGTH + " chars required");
        }

        for (String weak : KNOWN_WEAK_SECRETS) {
            if (secret.equals(weak)) {
                log.warn("═══════════════════════════════════════════════════════");
                log.warn("  检测到默认/示例 JWT 密钥！");
                log.warn("  当前密钥是已知的公开弱密钥，存在安全风险。");
                log.warn("  生产环境务必通过 JWT_SECRET 环境变量设置自定义密钥。");
                log.warn("  生成安全密钥：openssl rand -base64 32");
                log.warn("═══════════════════════════════════════════════════════");
                break;
            }
        }

        log.info("JWT 密钥校验通过（长度: {} 字符）", secret.length());
    }

    /**
     * 生成 Token
     */
    public String createToken(Long userId, String username) {
        return Jwts.builder()
                .claims(Map.of("userId", userId, "username", username))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取 Token 的 MD5 摘要（用于 Redis 黑名单 Key，避免存储完整 Token）
     */
    public String getTokenDigest(String token) {
        return org.springframework.util.DigestUtils.md5DigestAsHex(
                token.getBytes(StandardCharsets.UTF_8));
    }
}