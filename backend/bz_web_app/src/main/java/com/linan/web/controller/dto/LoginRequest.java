package com.linan.web.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录请求
 */
@Data
@Schema(name = "LoginRequest", description = "登录请求参数")
public class LoginRequest {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String captchaCode;

    @Schema(description = "验证码 UUID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uuid;
}