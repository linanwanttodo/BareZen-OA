package com.linan.web.controller;

import com.linan.core.common.annotation.RateLimit;
import com.linan.core.common.api.Result;
import com.linan.web.controller.dto.ChangePasswordRequest;
import com.linan.web.controller.dto.CurrentUserVO;
import com.linan.web.controller.dto.LoginRequest;
import com.linan.web.controller.dto.LoginVO;
import com.linan.web.service.CaptchaService;
import com.linan.web.service.IAuthService;
import com.linan.core.security.PassAuth;
import com.linan.core.security.interceptor.AuthInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 登录认证 Controller
 *
 * <p>仅负责参数绑定与 {@link Result} 委托，核心认证编排下沉至 {@link IAuthService}。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Tag(name = "登录认证", description = "用户登录、注册、密码管理")
@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final CaptchaService captchaService;

    /**
     * 用户登录
     * POST /api/auth/v1/login
     */
    @PassAuth
    @RateLimit(key = "auth:login", limit = 10, window = 60)
    @Operation(summary = "用户登录", responses = {
            @ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = LoginVO.class)))
    })
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 上传头像
     * POST /api/auth/v1/avatar
     */
    @RateLimit(key = "auth:avatar", limit = 5, window = 60)
    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @Parameter(description = "头像文件") @RequestParam("file") MultipartFile file) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        String avatar = authService.uploadAvatar(userId, file);
        return Result.success(Map.of("avatar", avatar));
    }

    /**
     * 用户注册
     * POST /api/auth/v1/register
     */
    @PassAuth
    @RateLimit(key = "auth:register", limit = 5, window = 60)
    @Operation(summary = "用户注册（受 auth.register-enabled 开关控制）")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody com.linan.system.entity.SysUser user) {
        authService.register(user.getUserName(), user.getPassword());
        return Result.success(null);
    }

    /**
     * 获取验证码
     * GET /api/auth/v1/captcha
     */
    @PassAuth
    @RateLimit(key = "auth:captcha", limit = 10, window = 60)
    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.success(captchaService.generate());
    }

    /**
     * 获取当前登录用户信息
     * GET /api/auth/v1/current/user
     */
    @Operation(summary = "获取当前用户信息", responses = {
            @ApiResponse(responseCode = "200", description = "当前用户资料",
                    content = @Content(schema = @Schema(implementation = CurrentUserVO.class)))
    })
    @GetMapping("/current/user")
    public Result<CurrentUserVO> getCurrentUser() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        return Result.success(authService.getCurrentUser(userId));
    }

    /**
     * 修改密码
     * PUT /api/auth/v1/password
     */
    @RateLimit(key = "auth:password", limit = 5, window = 60)
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        authService.changePassword(userId, request, authHeader);
        return Result.success(null);
    }

    /**
     * 用户退出登录
     * POST /api/auth/v1/logout
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return Result.success(null);
    }

    /**
     * Token 刷新
     * POST /api/auth/v1/refresh
     */
    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        String newToken = authService.refreshToken(userId, authHeader);
        return Result.success(Map.of("token", newToken));
    }
}