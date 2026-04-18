package com.linan.core.common.exception;

import com.linan.core.common.api.ResultCodeEnum;
import lombok.Getter;

/**
 * 业务异常
 *
 * 在 Service 层抛出此异常，由 GlobalExceptionHandler 统一处理，
 * 避免在每个方法中手动构造 Result.error() 返回。
 *
 * 使用方式：
 * throw new BusinessException("用户名已存在");
 * throw new BusinessException(ResultCodeEnum.FORBIDDEN);
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 状态码 */
    private final int code;

    /**
     * 使用自定义消息（默认状态码 500）
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCodeEnum.FAILED.getCode();
    }

    /**
     * 使用预定义状态码
     */
    public BusinessException(ResultCodeEnum errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    /**
     * 使用预定义状态码 + 自定义消息
     */
    public BusinessException(ResultCodeEnum errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}