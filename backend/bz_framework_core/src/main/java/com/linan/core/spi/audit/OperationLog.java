package com.linan.core.spi.audit;

import lombok.Data;

/**
 * 操作日志模型（core 层 SPI 传输对象）
 *
 * <p>由 {@link com.linan.core.common.aspect.OperLogAspect} 承载切面采集结果，
 * 经 {@link OperLogSink} 交由系统侧适配器落到 sys_oper_log。字段与
 * 持久化实体保持一致，避免 core 直接依赖系统实体。</p>
 *
 * @author BareZen Team
 */
@Data
public class OperationLog {

    /** 模块标题 */
    private String title;

    /** 业务类型 */
    private Integer businessType;

    /** HTTP 方法 */
    private String requestMethod;

    /** 操作人 */
    private String operUser;

    /** 操作 IP */
    private String operIp;

    /** 请求 URL */
    private String operUrl;

    /** 目标类全限定名 */
    private String className;

    /** 目标方法名 */
    private String methodName;

    /** 请求参数（已脱敏 JSON） */
    private String operParam;

    /** 响应结果 JSON */
    private String jsonResult;

    /** 状态：0=成功 1=失败 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costTime;
}