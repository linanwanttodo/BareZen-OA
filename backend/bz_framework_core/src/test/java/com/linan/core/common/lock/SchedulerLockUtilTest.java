package com.linan.core.common.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SchedulerLockUtil 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SchedulerLockUtilTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SchedulerLockUtil lockUtil;

    @BeforeEach
    void setUp() {
        lockUtil = new SchedulerLockUtil(redisTemplate);
    }

    @Nested
    @DisplayName("tryLock()")
    class TryLock {

        @Test
        @DisplayName("抢占成功返回令牌且带过期时间")
        void acquired_returnsToken() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(eq("scheduler:lock:test"), anyString(), eq(Duration.ofSeconds(30))))
                    .thenReturn(true);

            String token = lockUtil.tryLock("scheduler:lock:test", 30);

            assertNotNull(token);
            assertFalse(token.isBlank());
            verify(valueOperations).setIfAbsent(eq("scheduler:lock:test"), eq(token), eq(Duration.ofSeconds(30)));
        }

        @Test
        @DisplayName("抢占失败返回 null")
        void notAcquired_returnsNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

            String token = lockUtil.tryLock("scheduler:lock:test", 30);

            assertNull(token);
        }
    }

    @Nested
    @DisplayName("release()")
    class Release {

        @Test
        @DisplayName("令牌匹配时执行 Lua 原子删除")
        void tokenMatches_executesLuaDelete() {
            lockUtil.release("scheduler:lock:test", "token-abc");

            verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("scheduler:lock:test")), eq("token-abc"));
        }

        @Test
        @DisplayName("令牌为 null 时不触 Redis")
        void nullToken_doesNotTouchRedis() {
            lockUtil.release("scheduler:lock:test", null);

            verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
        }

        @Test
        @DisplayName("Redis 异常不向上抛出（TTL 兜底）")
        void redisError_swallows() {
            doThrow(new RuntimeException("redis down"))
                    .when(redisTemplate).execute(any(RedisScript.class), anyList(), any());

            assertDoesNotThrow(() -> lockUtil.release("scheduler:lock:test", "token-abc"));
        }
    }
}