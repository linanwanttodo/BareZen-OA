package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体，对应 sys_logininfor 表
 *
 * @author BareZen Team
 */
@Data
@TableName("sys_logininfor")
public class SysLogininfor {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户账号 */
    private String userName;

    /** 登录IP */
    private String ipAddr;

    /** 状态（0=成功 1=失败） */
    private String status;

    /** 提示消息 */
    private String msg;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;
}
