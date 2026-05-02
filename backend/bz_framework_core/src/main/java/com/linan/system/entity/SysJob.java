package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时任务实体，对应 sys_job 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
public class SysJob extends BaseEntity {
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    private String misfirePolicy;
    private String concurrent;
    private String status;
    private String remark;
}
