package com.linan.core.common.exception;

import com.linan.core.common.api.Result;
import com.linan.core.common.api.ResultCodeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;

/**
 * 全局异常处理器
 *
 * <p>统一处理所有类型的异常，返回标准化的 JSON 响应。
 * 处理优先级：子类异常优先于父类异常。</p>
 *
 * <p>异常处理策略：</p>
 * <ul>
 *   <li>业务异常：返回具体错误信息，日志级别为 WARN</li>
 *   <li>参数校验异常：返回校验失败详情，日志级别为 WARN</li>
 *   <li>权限异常：返回权限错误，日志级别为 WARN</li>
 *   <li>系统异常：返回通用错误信息，日志级别为 ERROR</li>
 * </ul>
 *
 * @author BareZen Team
 */
@Slf4j
@RestControllerAdvice
@Schema(description = "全局异常处理器，统一处理各类异常并返回标准响应格式")
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

    /**
     * 未匹配到任何处理器时 Spring 抛 NoResourceFoundException。此前落入兜底分支，
     * 探测性请求会返回 500 并在日志里打完整堆栈，掩盖真实故障。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoResource(NoResourceFoundException e) {
        log.warn("资源不存在: {} {}", e.getHttpMethod(), e.getResourcePath());
        return new Result<>(ResultCodeEnum.NOT_FOUND.getCode(), "资源不存在", null);
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
        return new Result<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), "请求参数不符合约束要求", null);
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<?> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据完整性约束违规: {}", e.getMessage());
        return new Result<>(ResultCodeEnum.FAILED.getCode(), "数据操作违反完整性约束", null);
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public Result<?> handleBadSql(BadSqlGrammarException e) {
        log.error("SQL 语法错误: {}", e.getMessage());
        return Result.error("数据库查询异常");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<?> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("请求路径不存在: {}", e.getRequestURL());
        return new Result<>(404, "请求路径不存在: " + e.getRequestURL(), null);
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
