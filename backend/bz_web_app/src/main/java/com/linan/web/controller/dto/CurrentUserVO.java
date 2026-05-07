package com.linan.web.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 当前用户信息
 */
@Data
@Schema(name = "CurrentUserVO", description = "当前登录用户资料")
public class CurrentUserVO {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "角色标识列表")
    private List<String> roles;

    @Schema(description = "权限标识列表")
    private Set<String> permissions;
}