package com.linan.web.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员重置用户密码请求
 *
 * <p>与个人改密（ChangePasswordRequest 需旧密码验证）不同，管理员重置仅需新密码；
 * 字段名 password 与前端 PUT /users/{id}/password 请求体约定一致。</p>
 */
@Data
@Schema(name = "ResetPasswordRequest", description = "重置密码请求参数")
public class ResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码（长度>=8，含大小写字母、数字与特殊字符）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
