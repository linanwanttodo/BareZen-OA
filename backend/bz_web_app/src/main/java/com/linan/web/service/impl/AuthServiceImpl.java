package com.linan.web.service.impl;

import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.JwtUtil;
import com.linan.core.security.PasswordUtil;
import com.linan.core.security.util.AuthCookieUtil;
import com.linan.core.common.util.IpUtil;
import com.linan.system.entity.SysLogininfor;
import com.linan.system.entity.SysRole;
import com.linan.system.entity.SysUser;
import com.linan.system.mapper.SysLogininforMapper;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.mapper.SysRoleMapper;
import com.linan.system.mapper.SysUserMapper;
import com.linan.system.monitor.OnlineUserService;
import com.linan.web.controller.dto.ChangePasswordRequest;
import com.linan.web.controller.dto.CurrentUserVO;
import com.linan.web.controller.dto.LoginRequest;
import com.linan.web.controller.dto.LoginVO;
import com.linan.web.service.CaptchaService;
import com.linan.web.service.IAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 认证业务实现
 *
 * <p>承载登录、锁定、改密、头像、当前用户等核心编排，Controller 仅做参数绑定与
 * {@code Result} 委托。所有业务失败以 {@link BusinessException} 抛出，交由
 * {@code GlobalExceptionHandler} 统一转 {@code Result}，不再于 Controller 内吞异常。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final OnlineUserService onlineUserService;
    private final JwtUtil jwtUtil;
    private final SysLogininforMapper logininforMapper;
    private final IpUtil ipUtil;
    private final CaptchaService captchaService;

    /** 是否开放注册，生产默认关闭 */
    @Value("${auth.register-enabled:false}")
    private boolean registerEnabled;

    /** 登录失败锁定阈值 */
    private static final int LOGIN_MAX_ATTEMPTS = 5;

    /** 单次失败基础延迟秒数（指数退避基数） */
    private static final int LOGIN_DELAY_BASE_SECONDS = 10;

    /** 最大延迟秒数（封顶值，避免无限增长） */
    private static final int LOGIN_MAX_DELAY_SECONDS = 3600; // 1小时

    /** 头像白名单与上限 */
    private static final Set<String> AVATAR_ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024L;

    /**
     * Lua 脚本：登录失败计数原子递增 + 指数退避延迟。
     * KEYS[1]=失败计数 key；ARGV[1]=基础延迟秒数；ARGV[2]=最大延迟秒数
     * 返回值=[当前失败次数, 应等待秒数]
     */
    private static final DefaultRedisScript<List> LOGIN_FAIL_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]) "
                    + "local baseDelay = tonumber(ARGV[1]) "
                    + "local maxDelay = tonumber(ARGV[2]) "
                    + "local delay = math.min(baseDelay * math.pow(2, current - 1), maxDelay) "
                    + "redis.call('EXPIRE', KEYS[1], math.ceil(delay)) "
                    + "return {current, math.floor(delay)} ",
            List.class
    );

    @Override
    public LoginVO login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String captchaCode = request.getCaptchaCode();
        String uuid = request.getUuid();

        String captchaError = captchaService.validate(uuid, captchaCode);
        if (captchaError != null) {
            throw new BusinessException(captchaError);
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException("密码不能为空");
        }

        // 获取客户端 IP（用于 IP 维度封禁）
        String clientIp = getClientIp();
        String ipFailKey = "ip_fail:" + clientIp;
        
        // IP 维度高频失败直接拒绝（防暴力破解）
        int ipFailCount = readFailCount(ipFailKey);
        if (ipFailCount >= LOGIN_MAX_ATTEMPTS * 3) { // IP 阈值为用户阈值的 3 倍
            recordLoginLog(username, "1", "IP 已被临时封禁");
            throw new BusinessException("操作过于频繁，请稍后再试");
        }

        // 账号维度指数退避延迟检查
        String accountFailKey = "account_fail:" + username;
        List<Object> failResult = readAccountFailWithDelay(accountFailKey);
        if (failResult != null && !failResult.isEmpty()) {
            long waitSeconds = ((Number) failResult.get(1)).longValue();
            if (waitSeconds > 0) {
                recordLoginLog(username, "1", "账号需等待 " + waitSeconds + " 秒");
                throw new BusinessException("登录失败次数过多，请 " + waitSeconds + " 秒后再试");
            }
        }

        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, username));
        
        // 用户名不存在时不持久化计数（统一延迟响应防枚举）
        if (user == null) {
            // 仅增加 IP 计数，不增加账号计数
            incrementIpFail(ipFailKey);
            recordLoginLog(username, "1", "用户不存在");
            // 统一延迟响应，防止用户名枚举
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            throw new BusinessException("用户名或密码错误");
        }
        
        if ("1".equals(user.getStatus())) {
            recordLoginLog(username, "1", "账号已停用");
            throw new BusinessException("该账号已被停用");
        }
        
        if (!PasswordUtil.matches(password, user.getPassword())) {
            // 密码错误时才记录账号失败计数
            recordAccountFailure(accountFailKey);
            // 同时增加 IP 计数
            incrementIpFail(ipFailKey);
            recordLoginLog(username, "1", "密码错误");
            throw new BusinessException("用户名或密码错误");
        }

        // 登录成功，清除所有失败计数
        redisTemplate.delete(accountFailKey);
        redisTemplate.delete(ipFailKey);
        recordLoginLog(username, "0", "登录成功");

        String token = jwtUtil.createToken(user.getId(), user.getUserName());
        String loginIp = getClientIp();
        onlineUserService.recordLogin(user.getId(), user.getUserName(), user.getNickName(), token, loginIp);
        setAuthCookie(token);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUserName(user.getUserName());
        vo.setNickName(user.getNickName());
        return vo;
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > AVATAR_MAX_SIZE) {
            throw new BusinessException("头像大小不能超过 2MB");
        }
        String originalName = file.getOriginalFilename();
        String suffix = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
                : ".jpg";
        if (!AVATAR_ALLOWED_EXT.contains(suffix)) {
            throw new BusinessException("仅支持 jpg/png/gif/webp 头像");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String dirPath = System.getProperty("user.dir") + "/uploads/avatar/" + datePath;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            file.transferTo(new File(dirPath + "/" + fileName));
        } catch (Exception e) {
            log.error("头像上传失败", e);
            throw new BusinessException("头像上传失败，请稍后重试");
        }

        String avatarPath = "/profile/avatar/" + datePath + "/" + fileName;
        SysUser user = new SysUser();
        user.setId(userId);
        user.setAvatar(avatarPath);
        userMapper.updateById(user);
        return avatarPath;
    }

    @Override
    public void register(String username, String password) {
        if (!registerEnabled) {
            throw new BusinessException("注册功能已关闭");
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }
        String policyMsg = PasswordUtil.validatePasswordPolicy(password);
        if (policyMsg != null) {
            throw new BusinessException(policyMsg);
        }

        // 无论是否存在均不抛出，防止枚举已注册用户
        Long count = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, username));
        if (count > 0) {
            return;
        }

        SysUser user = new SysUser();
        user.setUserName(username);
        user.setNickName(username);
        user.setPassword(PasswordUtil.encode(password));
        user.setStatus("0");
        user.setId(null);
        if (userMapper.insert(user) <= 0) {
            throw new BusinessException("注册失败");
        }
    }

    @Override
    public CurrentUserVO getCurrentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        List<SysRole> roles = roleMapper.selectRolesByUserId(userId);
        Set<String> permissions = new HashSet<>();
        for (SysRole role : roles) {
            permissions.addAll(menuMapper.selectPermsByRoleId(role.getId()));
        }

        CurrentUserVO vo = new CurrentUserVO();
        vo.setUserId(user.getId());
        vo.setUserName(user.getUserName());
        vo.setNickName(user.getNickName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(roles.stream().map(SysRole::getRoleKey).collect(Collectors.toList()));
        vo.setPermissions(permissions);
        return vo;
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request, String authHeader) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException("参数不完整");
        }
        String policyMsg = PasswordUtil.validatePasswordPolicy(newPassword);
        if (policyMsg != null) {
            throw new BusinessException(policyMsg);
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null || !PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }

        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(user);

        blacklistCurrentToken(authHeader);
        clearAuthCookie();
    }

    @Override
    public void logout(String authHeader) {
        blacklistCurrentToken(authHeader);
        clearAuthCookie();
    }

    @Override
    public String refreshToken(Long userId, String authHeader) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if ("1".equals(user.getStatus())) {
            throw new BusinessException("该账号已被停用");
        }

        blacklistCurrentToken(authHeader);

        String newToken = jwtUtil.createToken(user.getId(), user.getUserName());
        onlineUserService.recordLogin(user.getId(), user.getUserName(), user.getNickName(), newToken, getClientIp());
        setAuthCookie(newToken);
        return newToken;
    }

    private int readFailCount(String lockKey) {
        Object failCountObj = redisTemplate.opsForValue().get(lockKey);
        if (failCountObj instanceof Number) {
            return ((Number) failCountObj).intValue();
        }
        return 0;
    }

    private void recordLoginLog(String username, String status, String msg) {
        try {
            SysLogininfor loginLog = new SysLogininfor();
            loginLog.setUserName(username);
            loginLog.setStatus(status);
            loginLog.setMsg(msg != null && msg.length() > 200 ? msg.substring(0, 200) : msg);
            loginLog.setIpAddr(getClientIp());
            loginLog.setLoginTime(LocalDateTime.now());
            logininforMapper.insert(loginLog);
        } catch (Exception e) {
            // 静默处理，不影响登录流程
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            return ipUtil.getClientIp(attributes.getRequest());
        } catch (Exception e) {
            return "unknown";
        }
    }

    private void blacklistCurrentToken(String authHeader) {
        String token = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest().getCookies() != null) {
                for (Cookie cookie : attributes.getRequest().getCookies()) {
                    if ("bz_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }
        if (StringUtils.hasText(token)) {
            String digest = jwtUtil.getTokenDigest(token);
            redisTemplate.opsForValue().set("blacklist:" + digest, "invalidated", 24, TimeUnit.HOURS);
            onlineUserService.forceLogout(token);
        }
    }

    private void setAuthCookie(String token) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getResponse() == null) {
            return;
        }
        AuthCookieUtil.setAuthCookie(attributes.getResponse(), token);
    }

    private void clearAuthCookie() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getResponse() == null) {
            return;
        }
        AuthCookieUtil.clearAuthCookie(attributes.getResponse());
    }

    /**
     * 读取账号失败计数并计算指数退避延迟。
     * @return [failCount, delaySeconds]，若无记录返回 null
     */
    private List<Object> readAccountFailWithDelay(String accountFailKey) {
        try {
            Object value = redisTemplate.opsForValue().get(accountFailKey);
            if (value instanceof Number) {
                int failCount = ((Number) value).intValue();
                long delay = Math.min(
                    LOGIN_DELAY_BASE_SECONDS * (long) Math.pow(2, failCount - 1),
                    LOGIN_MAX_DELAY_SECONDS
                );
                return List.of(failCount, delay);
            }
        } catch (Exception e) {
            log.warn("读取账号失败计数失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 记录账号登录失败（仅递增计数，TTL 由 Lua 脚本管理）
     */
    private void recordAccountFailure(String accountFailKey) {
        try {
            stringRedisTemplate.execute(LOGIN_FAIL_SCRIPT, 
                List.of(accountFailKey), 
                String.valueOf(LOGIN_DELAY_BASE_SECONDS),
                String.valueOf(LOGIN_MAX_DELAY_SECONDS));
        } catch (Exception e) {
            log.warn("记录账号登录失败计数失败: {}", e.getMessage());
        }
    }

    /**
     * 记录 IP 登录失败（简单 INCR + EXPIRE）
     */
    private void incrementIpFail(String ipFailKey) {
        try {
            Long count = stringRedisTemplate.opsForValue().increment(ipFailKey);
            if (count != null && count == 1) {
                // 首次失败时设置 TTL（30分钟）
                stringRedisTemplate.expire(ipFailKey, 30, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.warn("记录 IP 登录失败计数失败: {}", e.getMessage());
        }
    }
}