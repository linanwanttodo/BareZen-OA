package com.linan.system.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linan.core.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final String ONLINE_USER_PREFIX = "online_user:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final long TOKEN_EXPIRATION_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    private final JwtUtil jwtUtil;

    @Override
    public void recordLogin(Long userId, String userName, String nickName,
                           String token, String loginIp) {
        String digest = jwtUtil.getTokenDigest(token);
        String key = ONLINE_USER_PREFIX + digest;

        OnlineUser onlineUser = new OnlineUser();
        onlineUser.setToken(token);
        onlineUser.setTokenDigest(digest);
        onlineUser.setUserId(userId);
        onlineUser.setUserName(userName);
        onlineUser.setNickName(nickName);
        onlineUser.setLoginIp(loginIp);
        onlineUser.setLoginTime(LocalDateTime.now());
        onlineUser.setBrowser(getBrowserInfo());
        onlineUser.setOs(getOsInfo());

        try {
            redisTemplate.opsForValue().set(key, onlineUser, TOKEN_EXPIRATION_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("记录在线用户失败: {}", e.getMessage());
        }
    }

    @Override
    public List<OnlineUser> listOnlineUsers() {
        List<OnlineUser> users = new ArrayList<>();
        try {
            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(ONLINE_USER_PREFIX + "*")
                    .count(100)
                    .build();
            try (Cursor<byte[]> cursor = redisTemplate.execute(
                    (org.springframework.data.redis.connection.RedisConnection connection) ->
                            connection.keyCommands().scan(scanOptions))) {
                if (cursor == null) {
                    return users;
                }
                while (cursor.hasNext()) {
                    String key = new String(cursor.next());
                    Object value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        OnlineUser user = convertToOnlineUser(value);
                        if (user != null) {
                            // 列表不回传完整 token
                            user.setToken(null);
                            users.add(user);
                        }
                    }
                }
            }

            users.sort((a, b) -> b.getLoginTime().compareTo(a.getLoginTime()));
        } catch (Exception e) {
            log.error("查询在线用户列表失败: {}", e.getMessage());
        }
        return users;
    }

    @Override
    public long countOnlineUsers() {
        long count = 0;
        try {
            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(ONLINE_USER_PREFIX + "*")
                    .count(100)
                    .build();
            try (Cursor<byte[]> cursor = redisTemplate.execute(
                    (org.springframework.data.redis.connection.RedisConnection connection) ->
                            connection.keyCommands().scan(scanOptions))) {
                if (cursor != null) {
                    while (cursor.hasNext()) {
                        cursor.next();
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("统计在线用户数失败: {}", e.getMessage());
            return 0;
        }
        return count;
    }

    @Override
    public void forceLogout(String tokenOrDigest) {
        if (tokenOrDigest == null || tokenOrDigest.isEmpty()) {
            return;
        }

        // 兼容：可能是完整 token，也可能是 digest
        String digest = tokenOrDigest.length() == 32 && tokenOrDigest.matches("[0-9a-fA-F]+")
                ? tokenOrDigest
                : jwtUtil.getTokenDigest(tokenOrDigest);

        String onlineKey = ONLINE_USER_PREFIX + digest;
        Object value = redisTemplate.opsForValue().get(onlineKey);
        OnlineUser onlineUser = convertToOnlineUser(value);

        // 删除在线记录
        redisTemplate.delete(onlineKey);

        // 将 JWT 加入黑名单（优先用完整 token 的 digest）
        String blacklistKey = BLACKLIST_PREFIX + digest;
        redisTemplate.opsForValue().set(blacklistKey, "force_logout", TOKEN_EXPIRATION_HOURS, TimeUnit.HOURS);

        // 若还能拿到完整 token，再按旧 key 兼容清理一次
        if (onlineUser != null && onlineUser.getToken() != null) {
            redisTemplate.delete(ONLINE_USER_PREFIX + onlineUser.getToken());
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + onlineUser.getToken(),
                    "force_logout",
                    TOKEN_EXPIRATION_HOURS,
                    TimeUnit.HOURS
            );
        }
    }

    @Override
    public boolean isOnline(String token) {
        String digest = jwtUtil.getTokenDigest(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_USER_PREFIX + digest))
                || Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_USER_PREFIX + token));
    }

    private OnlineUser convertToOnlineUser(Object value) {
        try {
            if (value == null) {
                return null;
            }
            if (value instanceof OnlineUser) {
                return (OnlineUser) value;
            }
            String json = objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json, OnlineUser.class);
        } catch (Exception e) {
            log.warn("转换在线用户对象失败: {}", e.getMessage());
            return null;
        }
    }

    private String getBrowserInfo() {
        return "Unknown";
    }

    private String getOsInfo() {
        return "Unknown";
    }
}
