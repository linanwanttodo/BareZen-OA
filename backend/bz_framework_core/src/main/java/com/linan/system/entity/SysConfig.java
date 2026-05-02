package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体，对应 sys_config 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {
    /** 参数名称 */
    private String configName;

    /** 参数键名 */
    private String configKey;

    /** 参数键值 */
    private String configValue;

    /** 系统内置（Y=是 N=否） */
    private String configType;

    /** 状态（0=停用 1=正常） */
    private String status;

    /** 备注 */
    private String remark;
}
