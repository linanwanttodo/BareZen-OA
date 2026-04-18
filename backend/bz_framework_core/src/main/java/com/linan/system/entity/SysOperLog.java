package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 操作日志实体，对应 sys_oper_log */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {
    private String title;
    private Integer businessType;
    private String method;
    private String requestMethod;
    private String operUser;
    private String operIp;
    private String operUrl;
    private String className;
    private String methodName;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private Long costTime;
    private java.util.Date operTime;
}