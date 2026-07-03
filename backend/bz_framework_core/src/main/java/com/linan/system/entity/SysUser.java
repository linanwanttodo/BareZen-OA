package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.linan.core.common.entity.BaseEntity;
import com.linan.core.common.validation.ValueInEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户实体类
 * 对应数据库表 sys_user
 *
 * <p>字段上的 {@link jakarta.validation} 注解用于 {@code @Valid} 请求参数校验。
 * 新增/更新操作会校验必填字段格式。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 登录用户名（必填，唯一） */
    @NotBlank(message = "用户名不能为空")
    private String userName;

    /** 用户昵称（必填） */
    @NotBlank(message = "用户昵称不能为空")
    private String nickName;

    /** 密码（BCrypt 存储；仅允许写入，响应中永不返回） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 邮箱（可留空；留空时不做格式校验） */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号（可留空；留空时不做格式校验） */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 性别：0=男 1=女 2=未知（与数据库 sys_user.sex COMMENT 保持一致） */
    @ValueInEnum(value = {"0", "1", "2"}, message = "性别值不在允许范围内")
    private String sex;

    /** 头像路径 */
    private String avatar;

    /** 状态：0=正常 1=停用（与 sys_user / 角色表约定一致） */
    @NotBlank(message = "状态不能为空")
    private String status;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    private String loginDate;

    /** 备注 */
    private String remark;
}