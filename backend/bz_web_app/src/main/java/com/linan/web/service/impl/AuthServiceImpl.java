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

    /** 锁定时间（分钟） */
    private static final int LOGIN_LOCK_MINUTES = 15;

    /** 头像白名单与上限 */
    private static final Set<String> AVATAR_ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024L;

    /**
     * Lua 脚本：登录失败计数原子递增（INCR 与首次 EXPIRE 同脚本内完成，避免竞态导致的永久残留）。
     * KEYS[1]=锁定计数 key；ARGV[1]=锁定分钟数（作为 TTL 秒）；返回值=当前失败次数。
     */
    private static final DefaultRedisScript<Long> LOGIN_FAIL_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]) "
                    + "if current == 1 then "
                    + "    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]) * 60) "
                    + "end "
                    + "return current",
            Long.class
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

        String lockKey = "account_lock:" + username;
        int failCount = readFailCount(lockKey);
        if (failCount >= LOGIN_MAX_ATTEMPTS) {
            recordLoginLog(username, "1", "账号已被锁定");
            throw new BusinessException("账号已被锁定，请 " + LOGIN_LOCK_MINUTES + " 分钟后再试");
        }

        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, username));
        if (user == null) {
            recordLoginFailure(lockKey);
            recordLoginLog(username, "1", "用户不存在");
            throw new BusinessException("用户名或密码错误");
        }
        if ("1".equals(user.getStatus())) {
            recordLoginLog(username, "1", "账号已停用");
            throw new BusinessException("该账号已被停用");
        }
        if (!PasswordUtil.matches(password, user.getPassword())) {
            recordLoginFailure(lockKey);
            recordLoginLog(username, "1", "密码错误");
            throw new BusinessException("用户名或密码错误");
        }

        redisTemplate.delete(lockKey);
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

    private void recordLoginFailure(String lockKey) {
        try {
            stringRedisTemplate.execute(LOGIN_FAIL_SCRIPT, List.of(lockKey), String.valueOf(LOGIN_LOCK_MINUTES));
        } catch (Exception e) {
            log.warn("记录登录失败计数失败: {}", e.getMessage());
        }
    }
}