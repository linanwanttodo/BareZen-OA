package com.linan.core.common.exception;

import com.linan.core.common.api.Result;
import com.linan.core.common.api.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 *
 * 统一处理所有类型的异常，返回标准化的 JSON 响应。
 * 处理优先级：子类异常优先于父类异常。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常 [code={}]: {}", e.getCode(), e.getMessage());
        return new Result<>(e.getCode(), e.getMessage(), null);
    }

    /**
     * 处理参数校验异常 - 请求体JSON格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), "请求体格式错误，请检查JSON格式", null);
    }

    /**
     * 处理参数校验异常 - 缺少必填参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParams(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getMessage());
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), "缺少必填参数: " + e.getParameterName(), null);
    }

    /**
     * 处理参数校验异常 - 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getMessage());
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(),
                "参数" + e.getName() + "类型不匹配", null);
    }

    /**
     * 处理 @Valid 参数校验失败（请求体字段校验）
     *
     * 当 Controller 参数使用 @Valid 注解且校验不通过时，
     * Spring 抛出 MethodArgumentNotValidException，在此统一转换为友好提示。
     * 返回格式：{"code":400, "msg":"userName: 用户名不能为空; email: 邮箱格式不正确", "data":null}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), message, null);
    }

    /**
     * 处理业务运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.warn("运行时异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理所有未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统未知异常: ", e);
        return Result.error("服务器开小差了，请稍后再试");
    }
}
