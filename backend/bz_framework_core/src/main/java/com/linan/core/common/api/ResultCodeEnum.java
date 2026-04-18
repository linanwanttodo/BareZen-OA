package com.linan.core.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnum {
    SUCCESS(200, "操作成功"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "没有访问权限"),
    VALIDATE_FAILED(400, "参数检验失败"),
    FAILED(500, "系统异常，请稍后重试");

    private final int code;
    private final String msg;
}
