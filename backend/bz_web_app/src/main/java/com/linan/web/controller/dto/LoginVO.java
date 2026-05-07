package com.linan.web.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录结果
 */
@Data
@Schema(name = "LoginVO", description = "登录成功返回")
public class LoginVO {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "昵称")
    private String nickName;
}