package com.linan.core.common.exception;

import com.linan.core.common.api.Result;
import com.linan.core.common.api.ResultCodeEnum;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;

/**
 * 全局异常处理器
 *
 * <p>统一处理所有类型的异常，返回标准化的 JSON 响应。
 * 处理优先级：子类异常优先于父类异常。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常 [code={}]: {}", e.getCode(), e.getMessage());
        return new Result<>(e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), "请求体格式错误，请检查JSON格式", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParams(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getMessage());
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), "缺少必填参数: " + e.getParameterName(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getMessage());
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), "参数" + e.getName() + "类型不匹配", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), message, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("参数约束违规: {}", e.getMessage());
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        log.warn("访问被拒绝: {}", e.getMessage());
        return new Result<>(ResultCodeEnum.FORBIDDEN.getCode(), "没有访问权限", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数非法: {}", e.getMessage());
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public Result<?> handleBadSql(BadSqlGrammarException e) {
        log.error("SQL 语法错误: {}", e.getMessage());
        return Result.error("数据库查询异常");
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.warn("运行时异常: ", e);
        return Result.error("服务器开小差了，请稍后再试");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统未知异常: ", e);
        return Result.error("服务器开小差了，请稍后再试");
    }
}
