package com.linan.web.service;

import com.linan.web.controller.dto.ChangePasswordRequest;
import com.linan.web.controller.dto.CurrentUserVO;
import com.linan.web.controller.dto.LoginRequest;
import com.linan.web.controller.dto.LoginVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 认证业务服务
 *
 * <p>收口登录、锁定、改密、头像、当前用户等核心认证编排，使 Controller 退化为
 * 纯参数绑定 + {@code Result} 委托，符合单一职责与面向接口原则。</p>
 */
public interface IAuthService {

    /**
     * 用户登录（含验证码、账号锁定、密码校验、在线记录与 Cookie 写入）
     *
     * @param request 登录参数
     * @return 登录结果（token 与用户概要）
     */
    LoginVO login(LoginRequest request);

    /**
     * 上传头像（白名单 + 大小校验 + 落盘 + 更新用户头像字段）
     *
     * @param userId  当前登录用户 ID
     * @param file    头像文件
     * @return 头像访问路径
     */
    String uploadAvatar(Long userId, MultipartFile file);

    /**
     * 注册（受 {@code auth.register-enabled} 开关控制）
     *
     * @param username 用户名
     * @param password 明文密码
     */
    void register(String username, String password);

    /**
     * 获取当前登录用户资料（含角色与权限标识）
     *
     * @param userId 当前登录用户 ID
     * @return 用户资料视图
     */
    CurrentUserVO getCurrentUser(Long userId);

    /**
     * 修改密码（校验旧密码 + 强策略 + 失效旧 Token/Cookie）
     *
     * @param userId      当前登录用户 ID
     * @param request     新旧密码
     * @param authHeader  Authorization 头（用于旧 Token 失效，可空）
     */
    void changePassword(Long userId, ChangePasswordRequest request, String authHeader);

    /**
     * 退出登录（失效旧 Token/Cookie）
     *
     * @param authHeader Authorization 头（用于旧 Token 失效，可空）
     */
    void logout(String authHeader);

    /**
     * 刷新 Token（失效旧 Token + 生成新 Token + 同步 Cookie）
     *
     * @param userId     当前登录用户 ID
     * @param authHeader Authorization 头（用于旧 Token 失效，可空）
     * @return 新 Token
     */
    String refreshToken(Long userId, String authHeader);
}