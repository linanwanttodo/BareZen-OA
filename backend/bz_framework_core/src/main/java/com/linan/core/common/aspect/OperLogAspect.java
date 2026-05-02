package com.linan.core.common.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysOperLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志 AOP 切面
 *
 * <p>负责拦截 @OperLog 注解，异步记录用户的操作行为到 sys_oper_log 表。
 * 增强功能：记录请求参数和响应结果。</p>
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    @Autowired
    private OperLogAsyncService operLogAsyncService;

    /** 敏感字段列表——记录日志时自动脱敏 */
    private static final List<String> SENSITIVE_FIELDS = List.of(
            "password", "oldPassword", "newPassword", "token",
            "idCard", "id_card", "idnumber", "idNumber",
            "phone", "mobile", "telphone", "telephone",
            "email", "bankCard", "bank_card", "bankAccount",
            "secret", "secretKey", "secret_key", "apiKey", "api_key",
            "accessKey", "access_key", "authCode", "auth_code"
    );

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(com.linan.core.common.annotation.OperLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperLog operLog = method.getAnnotation(OperLog.class);

        SysOperLog logEntity = new SysOperLog();
        logEntity.setTitle(operLog.title());
        logEntity.setBusinessType(operLog.businessType());
        logEntity.setClassName(joinPoint.getTarget().getClass().getName());
        logEntity.setMethodName(method.getName());

        if (request != null) {
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setOperUrl(request.getRequestURI());
            logEntity.setOperIp(getClientIp(request));
        }

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId != null) {
            logEntity.setOperUser(userId.toString());
        }

        // 记录请求参数（脱敏处理）
        String params = captureRequestParams(joinPoint.getArgs(), method);
        logEntity.setOperParam(params);

        Object result = null;
        try {
            result = joinPoint.proceed();
            logEntity.setStatus(0);
            // 记录响应结果（截断避免日志过大）
            logEntity.setJsonResult(truncate(toJsonString(result), 2000));
        } catch (Exception e) {
            logEntity.setStatus(1);
            logEntity.setErrorMsg(truncate(e.getMessage(), 2000));
            // 异常时也要记录部分结果
            logEntity.setJsonResult("{\"error\":\"" + truncate(e.getMessage(), 500) + "\"}");
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            logEntity.setCostTime(endTime - startTime);
            // 异步保存操作日志，不阻塞 API 响应
            operLogAsyncService.saveOperLog(logEntity);
        }

        return result;
    }

    /**
     * 捕获请求参数，脱敏后序列化为 JSON
     *
     * <p>支持以下类型的脱敏：</p>
     * <ul>
     *   <li>参数名称匹配敏感字段列表</li>
     *   <li>{@link Map} 类型参数的 key 递归匹配敏感字段列表</li>
     *   <li>支持嵌套 Map 的递归脱敏</li>
     * </ul>
     */
    private String captureRequestParams(Object[] args, Method method) {
        if (args == null || args.length == 0) {
            return "";
        }

        // 只捕获需要记录的业务参数（跳过 HttpServletRequest/Response、MultipartFile 等）
        Map<String, Object> paramMap = new LinkedHashMap<>();
        String[] paramNames = getParamNames(method);

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) continue;
            // 跳过 Servlet 容器相关的参数类型
            if (arg instanceof jakarta.servlet.ServletRequest
                    || arg instanceof jakarta.servlet.ServletResponse
                    || arg instanceof org.springframework.web.multipart.MultipartFile) {
                continue;
            }

            String name = i < paramNames.length ? paramNames[i] : "arg" + i;
            // 脱敏处理
            if (SENSITIVE_FIELDS.contains(name)) {
                paramMap.put(name, "****");
            } else if (arg instanceof Map) {
                // 对 Map 类型参数进行递归脱敏
                paramMap.put(name, desensitizeMap((Map<?, ?>) arg));
            } else {
                paramMap.put(name, arg);
            }
        }

        return paramMap.isEmpty() ? "" : toJsonString(paramMap);
    }

    /**
     * 递归脱敏 Map 类型参数
     *
     * <p>遍历 Map 的 entry，对 key 匹配敏感字段列表的值进行脱敏替换。
     * 支持嵌套 Map 的递归处理。</p>
     *
     * @param map 原始参数 Map
     * @return 脱敏后的新 Map（不会修改原始 Map）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> desensitizeMap(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey().toString() : "";
            Object value = entry.getValue();
            if (SENSITIVE_FIELDS.contains(key)) {
                result.put(key, "****");
            } else if (value instanceof Map) {
                // 递归处理嵌套 Map
                result.put(key, desensitizeMap((Map<?, ?>) value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * 获取方法参数名称列表
     */
    private String[] getParamNames(Method method) {
        try {
            java.lang.reflect.Parameter[] params = method.getParameters();
            String[] names = new String[params.length];
            for (int i = 0; i < params.length; i++) {
                names[i] = params[i].getName();
            }
            return names;
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * 将对象转为 JSON 字符串
     */
    private String toJsonString(Object obj) {
        if (obj == null) return "";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...(truncated)";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
