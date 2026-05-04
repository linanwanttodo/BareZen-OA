package com.linan.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.common.annotation.RateLimit;
import com.linan.core.common.api.Result;
import com.linan.core.common.util.IpUtil;
import com.linan.core.security.JwtUtil;
import com.linan.core.security.PassAuth;
import com.linan.core.security.PasswordUtil;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysLogininfor;
import com.linan.system.entity.SysRole;
import com.linan.system.entity.SysUser;
import com.linan.system.mapper.SysLogininforMapper;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.mapper.SysRoleMapper;
import com.linan.system.mapper.SysUserMapper;
import com.linan.system.monitor.OnlineUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 登录认证 Controller
 *
 * @author BareZen Team
 */
@Slf4j
@Tag(name = "登录认证", description = "用户登录、注册、密码管理")
@RestController
@RequestMapping("/api/auth/v1")
public class AuthController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final OnlineUserService onlineUserService;
    private final JwtUtil jwtUtil;
    private final SysLogininforMapper logininforMapper;
    private final IpUtil ipUtil;

    public AuthController(SysUserMapper userMapper, SysRoleMapper roleMapper,
                          SysMenuMapper menuMapper, RedisTemplate<String, Object> redisTemplate,
                          StringRedisTemplate stringRedisTemplate,
                          OnlineUserService onlineUserService, JwtUtil jwtUtil,
                          SysLogininforMapper logininforMapper, IpUtil ipUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.onlineUserService = onlineUserService;
        this.jwtUtil = jwtUtil;
        this.logininforMapper = logininforMapper;
        this.ipUtil = ipUtil;
    }

    /** 是否开放注册，生产默认关闭 */
    @Value("${auth.register-enabled:false}")
    private boolean registerEnabled;

    /** 登录失败锁定阈值 */
    private static final int LOGIN_MAX_ATTEMPTS = 5;

    /** 锁定时间（分钟） */
    private static final int LOGIN_LOCK_MINUTES = 15;

    /**
     * Lua 脚本：登录失败计数原子递增。
     * INCR 与首次 EXPIRE 在同一次脚本内完成，避免「先 INCR 后判 1 再 EXPIRE」
     * 的竞态窗口——并发失败请求交错时，key 可能永不设置 TTL，导致计数永久残留。
     *
     * KEYS[1] = 锁定计数 key
     * ARGV[1] = 锁定时间（分钟，作为 TTL 秒数）
     * 返回值：当前失败次数
     */
    private static final DefaultRedisScript<Long> LOGIN_FAIL_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]) " +
            "if current == 1 then " +
            "    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]) * 60) " +
            "end " +
            "return current",
            Long.class
    );

    private static final Set<String> AVATAR_ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 用户登录
     * POST /api/auth/v1/login
     */
    @PassAuth
    @RateLimit(key = "auth:login", limit = 10, window = 60)
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String captchaCode = body.get("captchaCode");
        String uuid = body.get("uuid");

        // 验证码校验
        if (uuid == null || uuid.isEmpty() || captchaCode == null || captchaCode.isEmpty()) {
            return Result.error("验证码不能为空");
        }
        String redisKey = "captcha:" + uuid;
        Object cachedCode = redisTemplate.opsForValue().get(redisKey);
        // 验证码一次性使用，无论对错都删除
        redisTemplate.delete(redisKey);
        if (cachedCode == null) {
            return Result.error("验证码已过期，请重新获取");
        }
        if (!cachedCode.toString().equalsIgnoreCase(captchaCode)) {
            return Result.error("验证码错误");
        }

        if (username == null || username.isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            return Result.error("密码不能为空");
        }

        // 账号锁定检查
        String lockKey = "account_lock:" + username;
        Object failCountObj = redisTemplate.opsForValue().get(lockKey);
        int failCount = 0;
        if (failCountObj instanceof Number) {
            failCount = ((Number) failCountObj).intValue();
        }
        if (failCount >= LOGIN_MAX_ATTEMPTS) {
            recordLoginLog(username, "1", "账号已被锁定");
            return Result.error("账号已被锁定，请 " + LOGIN_LOCK_MINUTES + " 分钟后再试");
        }

        // 查询用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, username));

        if (user == null) {
            recordLoginFailure(lockKey);
            recordLoginLog(username, "1", "用户不存在");
            return Result.error("用户名或密码错误");
        }

        // 先检查账号状态（在密码验证之前），避免已停用账号的密码被暴力尝试
        if ("1".equals(user.getStatus())) {
            recordLoginLog(username, "1", "账号已停用");
            return Result.error("该账号已被停用");
        }

        // 验证密码
        if (!PasswordUtil.matches(password, user.getPassword())) {
            recordLoginFailure(lockKey);
            recordLoginLog(username, "1", "密码错误");
            return Result.error("用户名或密码错误");
        }

        // 登录成功，清除失败计数
        redisTemplate.delete(lockKey);
        recordLoginLog(username, "0", "登录成功");

        // 生成 Token
        String token = jwtUtil.createToken(user.getId(), user.getUserName());

        // 记录在线用户（使用真实客户端 IP）
        String loginIp = getClientIp();
        onlineUserService.recordLogin(user.getId(), user.getUserName(), user.getNickName(), token, loginIp);

        // 设置 httpOnly Cookie（bz_token），降低 XSS 窃取 Token 风险
        setAuthCookie(token);

        // 返回 Token + 用户信息
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("userName", user.getUserName());
        data.put("nickName", user.getNickName());

        return Result.success(data);
    }

    /**
     * 上传头像
     * POST /api/auth/v1/avatar
     */
    @RateLimit(key = "auth:avatar", limit = 5, window = 60)
    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@Parameter(description = "头像文件") @RequestParam("file") MultipartFile file) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }

        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        if (file.getSize() > AVATAR_MAX_SIZE) {
            return Result.error("头像大小不能超过 2MB");
        }

        try {
            String originalName = file.getOriginalFilename();
            String suffix = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
                    : ".jpg";
            if (!AVATAR_ALLOWED_EXT.contains(suffix)) {
                return Result.error("仅支持 jpg/png/gif/webp 头像");
            }

            String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;

            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String dirPath = System.getProperty("user.dir") + "/uploads/avatar/" + datePath;
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(dirPath + "/" + fileName);
            file.transferTo(dest);

            // 头像访问路径（通过静态资源映射 /profile/ 提供）
            String avatarPath = "/profile/avatar/" + datePath + "/" + fileName;

            // 更新用户头像
            SysUser user = new SysUser();
            user.setId(userId);
            user.setAvatar(avatarPath);
            userMapper.updateById(user);

            Map<String, String> result = new HashMap<>();
            result.put("avatar", avatarPath);
            return Result.success(result);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败，请稍后重试");
        }
    }

    /**
     * 用户注册
     * POST /api/auth/v1/register
     * 默认关闭，需 auth.register-enabled=true 才可使用
     */
    @PassAuth
    @RateLimit(key = "auth:register", limit = 5, window = 60)
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody SysUser user) {
        if (!registerEnabled) {
            return Result.error("注册功能已关闭");
        }
        if (user.getUserName() == null || user.getUserName().isBlank()) {
            return Result.error("用户名不能为空");
        }
        // 密码强度策略：长度 >= 8 位，且包含大小写字母、数字与特殊字符
        String pwd = user.getPassword();
        String policyMsg = PasswordUtil.validatePasswordPolicy(pwd);
        if (policyMsg != null) {
            return Result.error(policyMsg);
        }
        if (user.getNickName() == null || user.getNickName().isBlank()) {
            user.setNickName(user.getUserName());
        }

        // 检查用户名是否已存在（无论是否存在均返回成功，防止枚举已注册用户）
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, user.getUserName()));
        if (count > 0) {
            return Result.success(null);
        }

        // 加密密码
        user.setPassword(PasswordUtil.encode(user.getPassword()));
        user.setStatus("0");
        user.setId(null);

        int rows = userMapper.insert(user);
        return rows > 0 ? Result.success(null) : Result.error("注册失败");
    }

    /**
     * 获取验证码
     * GET /api/auth/v1/captcha
     *
     * 返回一个简单的字符串验证码（4位数字字母）。
     * 生产环境建议使用滑动验证码或第三方服务（如腾讯防水墙）。
     */
    @PassAuth
    @RateLimit(key = "auth:captcha", limit = 10, window = 60)
    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        String code = generateCode(6);

        redisTemplate.opsForValue().set("captcha:" + uuid, code, 5, java.util.concurrent.TimeUnit.MINUTES);

        Map<String, String> data = new HashMap<>();
        data.put("uuid", uuid);
        data.put("captchaBase64", "data:image/svg+xml;base64," + generateSvgCaptcha(code));
        return Result.success(data);
    }

    private String generateCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 生成带干扰线、噪点和字符旋转扭曲的 SVG 验证码，防止 OCR 识别
     */
    private String generateSvgCaptcha(String code) {
        SecureRandom random = SECURE_RANDOM;
        int width = 160;
        int height = 55;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(width).append("' height='").append(height).append("'>");

        // 背景
        svg.append("<rect fill='#f0f0f0' width='").append(width).append("' height='").append(height).append("'/>");

        // 噪点 — 随机小圆点（增强：从80增加到150个）
        for (int i = 0; i < 150; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            String color = randomRgb(random);
            svg.append("<circle cx='").append(x).append("' cy='").append(y).append("' r='").append(random.nextBoolean() ? 1 : 2).append("' fill='").append(color).append("' opacity='0.7'/>");
        }

        // 干扰线 — 8条随机曲线（增强：从5条增加到8条）
        for (int i = 0; i < 8; i++) {
            String color = randomRgb(random);
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            int cx1 = random.nextInt(width);
            int cy1 = random.nextInt(height);
            svg.append("<path d='M").append(x1).append(",").append(y1)
               .append(" Q").append(cx1).append(",").append(cy1)
               .append(" ").append(x2).append(",").append(y2)
               .append("' stroke='").append(color).append("' stroke-width='1.5' fill='none' opacity='0.5'/>");
        }

        // 字符 — 每个字符随机旋转、偏移、颜色
        String[] colors = {"#333", "#2a5", "#c25", "#25c", "#c82", "#52c"};
        int charWidth = (width - 30) / code.length();
        for (int i = 0; i < code.length(); i++) {
            int x = 18 + i * charWidth + random.nextInt(8);
            int y = 30 + random.nextInt(12) - 6;
            int rotate = random.nextInt(40) - 20; // -20° ~ 20°
            String color = colors[random.nextInt(colors.length)];
            int fontSize = 22 + random.nextInt(6);
            svg.append("<text x='").append(x).append("' y='").append(y)
               .append("' font-size='").append(fontSize)
               .append("' fill='").append(color)
               .append("' font-family='monospace' font-weight='bold'")
               .append(" transform='rotate(").append(rotate).append(",").append(x).append(",").append(y).append(")'")
               .append(">").append(code.charAt(i)).append("</text>");
        }

        // 额外干扰弧线（增强：从3条增加到5条）
        for (int i = 0; i < 5; i++) {
            String color = randomRgb(random);
            svg.append("<line x1='").append(random.nextInt(width)).append("' y1='").append(random.nextInt(height))
               .append("' x2='").append(random.nextInt(width)).append("' y2='").append(random.nextInt(height))
               .append("' stroke='").append(color).append("' stroke-width='1' opacity='0.4'/>");
        }

        svg.append("</svg>");

        try {
            return Base64.getEncoder().encodeToString(svg.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(svg.toString().getBytes());
        }
    }

    private String randomRgb(SecureRandom random) {
        int r = random.nextInt(150) + 50;
        int g = random.nextInt(150) + 50;
        int b = random.nextInt(150) + 50;
        return "rgb(" + r + "," + g + "," + b + ")";
    }

    /**
     * 获取当前登录用户信息
     * GET /api/auth/v1/current/user
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/current/user")
    public Result<Map<String, Object>> getCurrentUser() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 查询用户角色
        List<SysRole> roles = roleMapper.selectRolesByUserId(userId);

        // 查询角色对应的权限标识
        Set<String> permissions = new HashSet<>();
        for (SysRole role : roles) {
            List<String> perms = menuMapper.selectPermsByRoleId(role.getId());
            permissions.addAll(perms);
        }

        // 组装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userName", user.getUserName());
        data.put("nickName", user.getNickName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("avatar", user.getAvatar());
        data.put("roles", roles.stream().map(SysRole::getRoleKey).collect(Collectors.toList()));
        data.put("permissions", permissions);

        return Result.success(data);
    }

    /**
     * 修改密码
     * PUT /api/auth/v1/password
     */
    @RateLimit(key = "auth:password", limit = 5, window = 60)
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return Result.error("参数不完整");
        }
        // 新密码强度策略：与注册接口保持一致（长度 >= 8 位，大小写字母+数字+特殊字符）
        String policyMsg = PasswordUtil.validatePasswordPolicy(newPassword);
        if (policyMsg != null) {
            return Result.error(policyMsg);
        }

        // 验证旧密码
        SysUser user = userMapper.selectById(userId);
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            return Result.error("旧密码不正确");
        }

        // 更新新密码
        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(user);

        // 密码修改成功后，将当前 Token 加入黑名单（Header 或 Cookie 双通道），使用户需要重新登录
        blacklistCurrentToken(authHeader, "password_changed");
        // 清除 httpOnly Cookie，强制用户重新登录
        clearAuthCookie();

        return Result.success(null);
    }

    /**
     * 用户退出登录
     * POST /api/auth/v1/logout
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Token 加入黑名单（Header 或 Cookie 双通道），黑名单使用 digest 避免存完整 JWT
        blacklistCurrentToken(authHeader, "logout");
        // 清除 httpOnly Cookie
        clearAuthCookie();
        return Result.success(null);
    }

    /**
     * Token刷新
     * POST /api/auth/v1/refresh
     */
    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if ("1".equals(user.getStatus())) {
            return Result.error("该账号已被停用");
        }

        // 旧 token 立即失效（Header 或 Cookie 双通道）
        blacklistCurrentToken(authHeader, "refresh");

        // 生成新 Token 并记录在线（使用真实客户端 IP）
        String newToken = jwtUtil.createToken(user.getId(), user.getUserName());
        onlineUserService.recordLogin(user.getId(), user.getUserName(), user.getNickName(), newToken, getClientIp());

        // 同步刷新 httpOnly Cookie
        setAuthCookie(newToken);

        Map<String, Object> data = new HashMap<>();
        data.put("token", newToken);

        return Result.success(data);
    }

    /**
     * 记录登录日志
     */
    private void recordLoginLog(String username, String status, String msg) {
        try {
            SysLogininfor loginLog = new SysLogininfor();
            loginLog.setUserName(username);
            loginLog.setStatus(status);
            loginLog.setMsg(msg != null && msg.length() > 200 ? msg.substring(0, 200) : msg);
            loginLog.setIpAddr(getClientIp());
            loginLog.setLoginTime(java.time.LocalDateTime.now());
            logininforMapper.insert(loginLog);
        } catch (Exception e) {
            // 静默处理，不影响登录流程
        }
    }

    /**
     * 获取客户端真实 IP 地址
     *
     * <p>统一委托 {@link IpUtil} 提取（默认仅取 RemoteAddr 防伪造，
     * 部署可信反向代理后可通过 {@code security.trust-proxy-headers} 开启代理头透传）。</p>
     *
     * @return 客户端 IP 地址，获取失败返回 "unknown"
     */
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

    /**
     * 将当前请求携带的 Token 加入黑名单（双通道：Authorization Header 优先，回退 httpOnly Cookie）
     *
     * <p>客户端登录后的标准鉴权通道是 httpOnly Cookie（bz_token）。此前登出/改密/刷新
     * 只拉黑 Header 中的 Bearer token，纯 Cookie 鉴权的会话在服务端旧 token 仍有效——
     * 已泄露的 token 无法立即失效。此处统一双通道拉黑（digest key，24h TTL），
     * 与 {@link AuthInterceptor} 的请求期黑名单校验路径对齐。</p>
     *
     * @param authHeader Authorization 请求头（可能为 null）
     * @param reason     拉黑原因：logout / password_changed / refresh
     */
    private void blacklistCurrentToken(String authHeader, String reason) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // 回退读取 httpOnly Cookie（bz_token）
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                Cookie[] cookies = attributes.getRequest().getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("bz_token".equals(cookie.getName())) {
                            token = cookie.getValue();
                            break;
                        }
                    }
                }
            }
        }
        if (token != null && !token.isEmpty()) {
            String digest = jwtUtil.getTokenDigest(token);
            // 黑名单使用 digest，避免存完整 JWT
            redisTemplate.opsForValue().set("blacklist:" + digest, reason, 24, TimeUnit.HOURS);
            onlineUserService.forceLogout(token);
        }
    }

    /**
     * 设置 httpOnly Cookie 携带 JWT Token
     *
     * <p>Cookie 属性：</p>
     * <ul>
     *   <li>{@code httpOnly=true}：禁止 JS 读取，降低 XSS 窃取风险</li>
     *   <li>{@code secure}：生产环境（HTTPS）启用，开发环境（HTTP）关闭</li>
     *   <li>{@code SameSite=Strict}：防御跨站请求伪造（CSRF）</li>
     *   <li>{@code path=/}、{@code maxAge} 与 JWT 过期时间一致</li>
     * </ul>
     *
     * @param token JWT Token
     */
    private void setAuthCookie(String token) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            jakarta.servlet.http.HttpServletResponse response = attributes.getResponse();
            if (response == null) {
                return;
            }
            // 通过请求 scheme 判断是否 HTTPS，决定 secure 标志
            HttpServletRequest req = attributes.getRequest();
            boolean secure = "https".equalsIgnoreCase(req.getScheme())
                    || "https".equalsIgnoreCase(req.getHeader("X-Forwarded-Proto"));
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("bz_token", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(secure);
            cookie.setPath("/");
            // SameSite=Strict 需通过响应头手动设置（Servlet 6.0 前 Cookie API 不支持）
            // maxAge 单位：秒；jwt 过期时间单位：毫秒
            cookie.setMaxAge((int) java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(
                    24L * 60 * 60 * 1000)); // 24 小时，与默认 JWT_EXPIRATION 对齐
            response.addCookie(cookie);
            // 显式补充 SameSite=Strict（兼容 Servlet 5/6）
            String sameSite = secure ? "SameSite=Strict" : "SameSite=Lax";
            // 已存在的 Set-Cookie 头由 addCookie 产生，此处再追加一条带 SameSite 的指令
            response.addHeader("Set-Cookie",
                    "bz_token=" + token + "; Path=/; Max-Age=86400; "
                            + (secure ? "Secure; " : "") + "HttpOnly; " + sameSite);
        } catch (Exception e) {
            log.warn("设置 httpOnly Cookie 失败: {}", e.getMessage());
        }
    }

    /**
     * 清除 httpOnly 认证 Cookie（登出 / 改密 时调用）
     */
    private void clearAuthCookie() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            jakarta.servlet.http.HttpServletResponse response = attributes.getResponse();
            if (response == null) {
                return;
            }
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("bz_token", "");
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(0); // 立即删除
            response.addCookie(cookie);
            response.addHeader("Set-Cookie",
                    "bz_token=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
        } catch (Exception e) {
            log.warn("清除 httpOnly Cookie 失败: {}", e.getMessage());
        }
    }

    /**
     * 记录登录失败计数（Lua 原子递增 + TTL 设置）
     */
    private void recordLoginFailure(String lockKey) {
        try {
            stringRedisTemplate.execute(LOGIN_FAIL_SCRIPT, List.of(lockKey), String.valueOf(LOGIN_LOCK_MINUTES));
        } catch (Exception e) {
            // 计数失败不阻断登录主流程，仅记录日志
            log.warn("记录登录失败计数失败: {}", e.getMessage());
        }
    }
}