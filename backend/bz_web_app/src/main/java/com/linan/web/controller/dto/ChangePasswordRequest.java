package com.linan.web.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 修改密码请求
 */
@Data
@Schema(name = "ChangePasswordRequest", description = "修改密码请求参数")
public class ChangePasswordRequest {

    @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @Schema(description = "新密码（长度>=8，含大小写字母、数字与特殊字符）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}