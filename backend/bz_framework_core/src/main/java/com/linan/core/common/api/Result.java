package com.linan.core.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

/**
 * 统一API返回体
 *
 * 所有 Controller 方法的返回值都必须使用 Result<T> 包装。
 * T 为 data 的实际数据类型，可以是：
 * - 普通对象：Result<User>
 * - 集合：Result<List<User>>
 * - 分页对象：Result<PageResult<User>>
 * - 无数据：Result<Void>（此时 data 为 null）
 *
 * @param <T> 数据类型（泛型）
 */
@Data
public class Result<T> {

    /** 状态码：200 表示成功，其他值表示异常 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 返回数据 */
    private T data;

    /**
     * 无参构造函数
     * public 修饰，因为：
     * 1. Jackson 反序列化时需要无参构造器
     * 2. 拦截器等跨包组件需要构造 Result 对象
     */
    public Result() {}

    /**
     * 全参构造函数
     *
     * @param code 状态码
     * @param msg  提示信息
     * @param data 返回数据
     */
    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功响应 — 使用默认成功信息
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMsg(), data);
    }

    /**
     * 成功响应 — 分页查询结果
     *
     * 将 MyBatis-Plus 的 IPage 自动包装为 PageResult，
     * 避免直接暴露 IPage 的内部字段给前端。
     */
    public static <T> Result<PageResult<T>> page(IPage<T> page) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMsg(),
                PageResult.from(page));
    }

    /**
     * 成功响应 — 使用自定义成功信息
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), msg, data);
    }

    /**
     * 失败响应 — 使用预定义状态码
     */
    public static <T> Result<T> error(ResultCodeEnum errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    /**
     * 失败响应 — 使用自定义错误信息
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(ResultCodeEnum.FAILED.getCode(), msg, null);
    }
}