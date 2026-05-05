package com.linan.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤打卡记录实体，对应 att_clock_record 表
 *
 * <p>同一用户同日仅一行：上班打卡创建该行，下班打卡更新该行；
 * 迟到/早退分钟由服务端按规则计算后落库。</p>
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_clock_record")
public class AttClockRecord extends BaseEntity {

    /** 打卡人用户ID */
    private Long userId;

    /** 打卡人姓名（冗余） */
    private String userName;

    /** 打卡人部门（冗余，统计场景由服务端补齐） */
    private String deptName;

    /** 打卡日期 */
    private LocalDate clockDate;

    /** 上班打卡时间 */
    private LocalDateTime clockInTime;

    /** 下班打卡时间 */
    private LocalDateTime clockOutTime;

    /** 迟到分钟数（服务端计算） */
    private Integer lateMinutes;

    /** 早退分钟数（服务端计算） */
    private Integer earlyMinutes;

    /** 状态：normal=正常 late=迟到 early=早退 absent=缺卡 abnormal=异常 */
    private String status;
}