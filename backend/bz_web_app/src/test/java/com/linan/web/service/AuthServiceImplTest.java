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
        lenient().when(redisTemplate.opsForValue()).thenReturn(
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class));
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
        when(redisTemplate.opsForValue().get("account_lock:admin")).thenReturn(null);
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
    @DisplayName("登录失败：账号已锁定时抛异常")
    void login_locked() {
        when(captchaService.validate(anyString(), anyString())).thenReturn(null);
        when(redisTemplate.opsForValue().get("account_lock:admin"))
                .thenReturn(10); // >= LOGIN_MAX_ATTEMPTS(5)

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");
        req.setCaptchaCode("x");
        req.setUuid("u");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals("账号已被锁定，请 15 分钟后再试", ex.getMessage());
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