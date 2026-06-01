package com.linan.web.service.impl;

import com.linan.core.security.JwtUtil;
import com.linan.core.security.PasswordUtil;
import com.linan.core.security.util.AuthCookieUtil;
import com.linan.core.common.exception.BusinessException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthServiceImpl 单元测试（登录/锁定/改密/当前用户下沉后的行为校验）
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private com.linan.core.common.api.Result<?> ignored;
    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    @Mock
    private OnlineUserService onlineUserService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private SysLogininforMapper logininforMapper;
    @Mock
    private IpUtil ipUtil;
    @Mock
    private CaptchaService captchaService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthServiceImpl(userMapper, roleMapper, menuMapper, redisTemplate,
                stringRedisTemplate, onlineUserService, jwtUtil, logininforMapper, ipUtil, captchaService);
        // 开启注册开关，便于注册用例
        java.lang.reflect.Field f = AuthServiceImpl.class.getDeclaredField("registerEnabled");
        f.setAccessible(true);
        f.set(authService, true);

        lenient().when(ipUtil.getClientIp(any())).thenReturn("127.0.0.1");
        
        // Mock Redis ValueOperations (lenient to avoid strict stubbing issues)
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps = 
            org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenReturn(null);
        
        // Mock StringRedisTemplate operations for Lua script
        lenient().when(stringRedisTemplate.execute(any(), any(), any(String[].class)))
            .thenReturn(java.util.List.of(0L, 0L));
        lenient().when(stringRedisTemplate.opsForValue())
            .thenReturn(org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class));
    }

    @Test
    @DisplayName("登录成功：返回 token 与用户概要")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");
        req.setCaptchaCode("abcd");
        req.setUuid("u1");

        when(captchaService.validate("u1", "abcd")).thenReturn(null);
        
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUserName("admin");
        user.setNickName("管理员");
        user.setStatus("0");
        user.setPassword(PasswordUtil.encode("admin123"));
        when(userMapper.selectOne(any())).thenReturn(user);
        when(jwtUtil.createToken(1L, "admin")).thenReturn("tok-123");

        LoginVO vo = authService.login(req);

        assertEquals("tok-123", vo.getToken());
        assertEquals(1L, vo.getUserId());
        verify(onlineUserService).recordLogin(eq(1L), eq("admin"), eq("管理员"), eq("tok-123"), anyString());
        verify(logininforMapper).insert(any(SysLogininfor.class));
    }

    @Test
    @DisplayName("登录失败：验证码错误时直接抛业务异常")
    void login_captchaError() {
        when(captchaService.validate(anyString(), anyString())).thenReturn("验证码错误");
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");
        req.setCaptchaCode("x");
        req.setUuid("u");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals("验证码错误", ex.getMessage());
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("登录失败：账号已锁定时抛异常（指数退避）")
    void login_locked() {
        when(captchaService.validate(anyString(), anyString())).thenReturn(null);

        // 账号失败计数已存在：5 次失败 → 退避 10 * 2^4 = 160 秒。
        // 实现从 redisTemplate 读取 account_fail:<username> 判定锁定，锁定时不得查询用户。
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps = redisTemplate.opsForValue();
        when(valueOps.get("account_fail:admin")).thenReturn(5L);

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");
        req.setCaptchaCode("x");
        req.setUuid("u");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertTrue(ex.getMessage().contains("登录失败次数过多"),
                "锁定时应抛出退避提示，实际: " + ex.getMessage());
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("登录失败：IP被封禁时抛异常")
    void login_ipBanned() {
        when(captchaService.validate(anyString(), anyString())).thenReturn(null);
        
        // Mock IP fail count >= 15 (LOGIN_MAX_ATTEMPTS * 3)
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps = redisTemplate.opsForValue();
        when(valueOps.get(anyString())).thenReturn(20);

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");
        req.setCaptchaCode("x");
        req.setUuid("u");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals("操作过于频繁，请稍后再试", ex.getMessage());
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("修改密码：旧密码错误时抛异常")
    void changePassword_wrongOld() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setPassword(PasswordUtil.encode("oldPass1!"));
        when(userMapper.selectById(2L)).thenReturn(user);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("NewPass1!");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.changePassword(2L, req, "Bearer t"));
        assertEquals("旧密码不正确", ex.getMessage());
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("修改密码：新密码不满足强策略时抛异常")
    void changePassword_weakPolicy() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass1!");
        req.setNewPassword("123"); // 弱

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.changePassword(2L, req, "Bearer t"));
        assertEquals("密码长度不能少于8位", ex.getMessage());
    }

    @Test
    @DisplayName("当前用户：聚合角色与权限标识")
    void getCurrentUser_aggregate() {
        SysUser user = new SysUser();
        user.setId(3L);
        user.setUserName("alice");
        user.setNickName("Alice");
        user.setEmail("a@b.com");
        user.setPhone("13800000000");
        user.setAvatar("/profile/avatar/a.png");
        when(userMapper.selectById(3L)).thenReturn(user);

        SysRole role = new SysRole();
        role.setId(10L);
        role.setRoleKey("common");
        when(roleMapper.selectRolesByUserId(3L)).thenReturn(List.of(role));
        when(menuMapper.selectPermsByRoleId(10L)).thenReturn(List.of("system:user:list"));

        CurrentUserVO vo = authService.getCurrentUser(3L);

        assertEquals("alice", vo.getUserName());
        assertEquals(List.of("common"), vo.getRoles());
        assertEquals(Set.of("system:user:list"), vo.getPermissions());
    }

    @Test
    @DisplayName("头像上传：写入 barezen.upload-path 配置目录而非 user.dir")
    void uploadAvatar_usesConfiguredUploadPath(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        // 注入自定义上传根目录（生产由 Spring 填充 barezen.upload-path）
        java.lang.reflect.Field f = AuthServiceImpl.class.getDeclaredField("uploadPath");
        f.setAccessible(true);
        f.set(authService, tempDir.toString());

        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        String avatarPath = authService.uploadAvatar(1L, file);

        assertEquals(true, avatarPath.startsWith("/profile/avatar/"));
        String relative = avatarPath.substring("/profile/".length());
        java.io.File written = tempDir.resolve(relative).toFile();
        assertEquals(true, written.exists(),
                "头像应写入配置的上传目录，实际文件不存在: " + written.getAbsolutePath());
    }

    @Test
    @DisplayName("注册：关闭开关时抛异常")
    void register_disabled() throws Exception {
        java.lang.reflect.Field f = AuthServiceImpl.class.getDeclaredField("registerEnabled");
        f.setAccessible(true);
        f.set(authService, false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register("bob", "NewPass1!"));
        assertEquals("注册功能已关闭", ex.getMessage());
        verify(userMapper, never()).insert(any(SysUser.class));
    }
}