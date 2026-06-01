package com.linan.web.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.web.controller.dto.ChangePasswordRequest;
import com.linan.web.controller.dto.CurrentUserVO;
import com.linan.web.controller.dto.LoginRequest;
import com.linan.web.controller.dto.LoginVO;
import com.linan.web.service.CaptchaService;
import com.linan.web.service.IAuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthController 登录认证单元测试
 *
 * <p>核心认证编排下沉至 {@link IAuthService}，本测试聚焦 Controller 的参数绑定
 * 与 Result 委托。AuthInterceptor 当前用户通过 {@link #setThreadLocal} 注入。</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private IAuthService authService;

    @Mock
    private CaptchaService captchaService;

    private AuthController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new AuthController(authService, captchaService);
        setThreadLocal("currentUserIdHolder", 1L);
    }

    @AfterEach
    void tearDown() throws Exception {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("login 委托认证服务并返回 LoginVO")
    void login_delegatesToAuthService() {
        LoginRequest request = new LoginRequest();
        LoginVO expected = new LoginVO();
        expected.setToken("jwt-token");
        when(authService.login(request)).thenReturn(expected);

        Result<LoginVO> result = controller.login(request);

        assertEquals(200, result.getCode());
        assertEquals("jwt-token", result.getData().getToken());
        verify(authService).login(request);
    }

    @Test
    @DisplayName("uploadAvatar 上传头像成功")
    void uploadAvatar_success() {
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "fake-image".getBytes());
        when(authService.uploadAvatar(1L, file)).thenReturn("/uploads/avatar.png");

        Result<Map<String, String>> result = controller.uploadAvatar(file);

        assertEquals(200, result.getCode());
        assertEquals("/uploads/avatar.png", result.getData().get("avatar"));
        verify(authService).uploadAvatar(1L, file);
    }

    @Test
    @DisplayName("register 委托注册服务")
    void register_delegatesToAuthService() {
        com.linan.system.entity.SysUser user = new com.linan.system.entity.SysUser();
        user.setUserName("newuser");
        user.setPassword("Str0ng!pass");

        Result<Void> result = controller.register(user);

        assertEquals(200, result.getCode());
        verify(authService).register("newuser", "Str0ng!pass");
    }

    @Test
    @DisplayName("captcha 返回验证码")
    void captcha_returnsCaptcha() {
        Map<String, String> captchaMap = Map.of("uuid", "abc-123", "captchaBase64", "data:image/svg+xml;base64,...");
        when(captchaService.generate()).thenReturn(captchaMap);

        Result<Map<String, String>> result = controller.captcha();

        assertEquals(200, result.getCode());
        assertEquals("abc-123", result.getData().get("uuid"));
        verify(captchaService).generate();
    }

    @Test
    @DisplayName("getCurrentUser 返回当前用户信息")
    void getCurrentUser_success() {
        CurrentUserVO vo = new CurrentUserVO();
        vo.setUserId(1L);
        vo.setUserName("admin");
        vo.setPermissions(Set.of("system:user:list"));
        when(authService.getCurrentUser(1L)).thenReturn(vo);

        Result<CurrentUserVO> result = controller.getCurrentUser();

        assertEquals(200, result.getCode());
        assertEquals(1L, result.getData().getUserId());
        assertEquals("admin", result.getData().getUserName());
        verify(authService).getCurrentUser(1L);
    }

    @Test
    @DisplayName("changePassword 修改密码")
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass1!");
        request.setNewPassword("newPass1!");
        String authHeader = "Bearer old-token";

        Result<Void> result = controller.changePassword(request, authHeader);

        assertEquals(200, result.getCode());
        verify(authService).changePassword(1L, request, authHeader);
    }

    @Test
    @DisplayName("logout 退出登录")
    void logout_success() {
        Result<Void> result = controller.logout("Bearer token");

        assertEquals(200, result.getCode());
        verify(authService).logout("Bearer token");
    }

    @Test
    @DisplayName("refreshToken 刷新 Token")
    void refreshToken_success() {
        when(authService.refreshToken(1L, "Bearer old-token")).thenReturn("new-jwt-token");

        Result<Map<String, Object>> result = controller.refreshToken("Bearer old-token");

        assertEquals(200, result.getCode());
        assertEquals("new-jwt-token", result.getData().get("token"));
        verify(authService).refreshToken(1L, "Bearer old-token");
    }
}