package com.linan.core.common.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 调度器分布式锁工具。
 *
 * <p>设计要点：
 * <ul>
 *   <li>SET NX EX 原子抢占，返回唯一令牌；抢占失败返回 null，调用方应直接跳过本轮执行；</li>
 *   <li>释放时走 Lua 脚本比对令牌后再删除，杜绝误删其他实例新获取的锁；</li>
 *   <li>释放异常仅记录告警，锁会随 TTL 自然过期兜底，不向上抛中断业务。</li>
 * </ul>
 */
@Slf4j
@Component
public class SchedulerLockUtil {

    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "  return redis.call('del', KEYS[1]) "
                    + "else "
                    + "  return 0 "
                    + "end";

    private final StringRedisTemplate redisTemplate;

    public SchedulerLockUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取分布式锁。
     *
     * @param key           锁键，如 scheduler:lock:approval_timeout
     * @param expireSeconds 锁过期时间（秒），作为异常兜底；实际持锁时间远短于此
     * @return 锁令牌；获取失败（已被其他实例持有）返回 null
     */
    public String tryLock(String key, long expireSeconds) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, token, Duration.ofSeconds(expireSeconds));
        if (Boolean.TRUE.equals(acquired)) {
            return token;
        }
        return null;
    }

    /**
     * 释放锁：仅当令牌与当前持有者匹配时删除。
     *
     * @param key   锁键
     * @param token {@link #tryLock} 返回的令牌；null 表示未持有锁，直接返回
     */
    public void release(String key, String token) {
        if (token == null) {
            return;
        }
        try {
            redisTemplate.execute(
                    new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class),
                    List.of(key),
                    token);
        } catch (Exception e) {
            // 释放失败仅记录告警，锁会随 TTL 自然过期兜底
            log.warn("释放调度器锁失败, key={}, err={}", key, e.getMessage());
        }
    }
}