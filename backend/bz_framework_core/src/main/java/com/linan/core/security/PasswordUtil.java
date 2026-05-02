package com.linan.core.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordUtil {

    private static final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 强密码策略校验（统一规则，供注册/改密/批量建号复用）
     *
     * <p>规则：长度 ≥ 8 位，且同时包含大写字母、小写字母、数字、特殊字符。</p>
     *
     * @param rawPassword 明文密码
     * @return 校验通过返回 null，否则返回错误提示文案
     */
    public static String validatePasswordPolicy(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return "密码不能为空";
        }
        if (rawPassword.length() < 8) {
            return "密码长度不能少于8位";
        }
        if (!rawPassword.matches(".*[A-Z].*")) {
            return "密码必须包含大写字母";
        }
        if (!rawPassword.matches(".*[a-z].*")) {
            return "密码必须包含小写字母";
        }
        if (!rawPassword.matches(".*\\d.*")) {
            return "密码必须包含数字";
        }
        if (!rawPassword.matches(".*[^A-Za-z0-9].*")) {
            return "密码必须包含特殊字符";
        }
        return null;
    }
}