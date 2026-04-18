package com.linan.system.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final String ONLINE_USER_PREFIX = "online_user:";
    private static final long TOKEN_EXPIRATION_HOURS = 24;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void recordLogin(Long userId, String userName, String nickName,
                           String token, String loginIp) {
        String key = ONLINE_USER_PREFIX + token;

        OnlineUser onlineUser = new OnlineUser();
        onlineUser.setToken(token);
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
            Set<String> keys = redisTemplate.keys(ONLINE_USER_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return users;
            }

            for (String key : keys) {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    OnlineUser user = convertToOnlineUser(value);
                    if (user != null) {
                        users.add(user);
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
        try {
            Set<String> keys = redisTemplate.keys(ONLINE_USER_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("统计在线用户数失败: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public void forceLogout(String token) {
        String key = ONLINE_USER_PREFIX + token;
        redisTemplate.delete(key);
    }

    @Override
    public boolean isOnline(String token) {
        String key = ONLINE_USER_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private OnlineUser convertToOnlineUser(Object value) {
        try {
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
