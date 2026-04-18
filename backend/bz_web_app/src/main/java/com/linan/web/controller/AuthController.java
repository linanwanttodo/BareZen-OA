package com.linan.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.common.api.Result;
import com.linan.core.security.JwtUtil;
import com.linan.core.security.PassAuth;
import com.linan.core.security.PasswordUtil;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysRole;
import com.linan.system.entity.SysUser;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.mapper.SysRoleMapper;
import com.linan.system.mapper.SysUserMapper;
import com.linan.system.monitor.OnlineUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 登录认证 Controller
 *
 * @author BareZen Team
 */
@RestController
@RequestMapping("/api/auth/v1")
public class AuthController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     * POST /api/auth/v1/login
     */
    @PassAuth
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

        // 查询用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, username));

        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 验证密码
        if (!PasswordUtil.matches(password, user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // 检查状态（数据库：0=正常 1=停用）
        if ("1".equals(user.getStatus())) {
            return Result.error("该账号已被停用");
        }

        // 生成 Token
        String token = jwtUtil.createToken(user.getId(), user.getUserName());

        // 记录在线用户
        String loginIp = "127.0.0.1";
        onlineUserService.recordLogin(user.getId(), user.getUserName(), user.getNickName(), token, loginIp);

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
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }

        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            String originalName = file.getOriginalFilename();
            String suffix = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : ".jpg";

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
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 用户注册
     * POST /api/auth/v1/register
     */
    @PassAuth
    @PostMapping("/register")
    public Result<Void> register(@RequestBody SysUser user) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, user.getUserName()));
        if (count > 0) {
            return Result.error("用户名已存在");
        }

        // 加密密码
        user.setPassword(PasswordUtil.encode(user.getPassword()));
        user.setStatus("0");

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
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        String code = generateCode(4);

        redisTemplate.opsForValue().set("captcha:" + uuid, code, 5, java.util.concurrent.TimeUnit.MINUTES);

        Map<String, String> data = new HashMap<>();
        data.put("uuid", uuid);
        data.put("captchaBase64", "data:image/svg+xml;base64," + generateSvgCaptcha(code));
        return Result.success(data);
    }

    private String generateCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 生成带干扰线、噪点和字符旋转扭曲的 SVG 验证码，防止 OCR 识别
     */
    private String generateSvgCaptcha(String code) {
        java.util.Random random = new java.util.Random();
        int width = 150;
        int height = 50;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(width).append("' height='").append(height).append("'>");

        // 背景
        svg.append("<rect fill='#f0f0f0' width='").append(width).append("' height='").append(height).append("'/>");

        // 噪点 — 随机小圆点
        for (int i = 0; i < 80; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            String color = randomRgb(random);
            svg.append("<circle cx='").append(x).append("' cy='").append(y).append("' r='1' fill='").append(color).append("' opacity='0.6'/>");
        }

        // 干扰线 — 5条随机曲线
        for (int i = 0; i < 5; i++) {
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

        // 额外干扰弧线
        for (int i = 0; i < 3; i++) {
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

    private String randomRgb(java.util.Random random) {
        int r = random.nextInt(150) + 50;
        int g = random.nextInt(150) + 50;
        int b = random.nextInt(150) + 50;
        return "rgb(" + r + "," + g + "," + b + ")";
    }

    /**
     * 获取当前登录用户信息
     * GET /api/auth/v1/current/user
     */
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
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return Result.error("参数不完整");
        }

        // 验证旧密码
        SysUser user = userMapper.selectById(userId);
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            return Result.error("旧密码不正确");
        }

        // 更新新密码
        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(user);

        return Result.success(null);
    }

    /**
     * 用户退出登录
     * POST /api/auth/v1/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // 将 Token 加入黑名单，设置过期时间与 JWT 一致（24小时）
            redisTemplate.opsForValue().set("blacklist:" + token, "logout", 24, java.util.concurrent.TimeUnit.HOURS);
        }
        return Result.success(null);
    }

    /**
     * Token刷新
     * POST /api/auth/v1/refresh
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 生成新 Token
        String newToken = jwtUtil.createToken(user.getId(), user.getUserName());

        Map<String, Object> data = new HashMap<>();
        data.put("token", newToken);

        return Result.success(data);
    }
}