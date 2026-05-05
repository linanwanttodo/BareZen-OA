package com.linan.hr.domain;

import lombok.Data;

/**
 * 月度考勤统计视图（SQL GROUP BY 聚合结果，非表实体）
 *
 * <p>口径：workDays=该月有打卡记录的天数；lateCount=迟到天数（late/abnormal）；
 * earlyCount=早退天数（early/abnormal）；absentCount=缺卡天数（只打了上班或下班其中一次）。</p>
 *
 * @author BareZen Team
 */
@Data
public class AttClockMonthlyStat {

    /** 员工用户ID */
    private Long userId;

    /** 员工姓名（冗余） */
    private String userName;

    /** 所属部门（关联 sys_dept 取最大名称，员工多部门时展示其一） */
    private String deptName;

    /** 出勤天数 */
    private Long workDays;

    /** 迟到次数 */
    private Long lateCount;

    /** 早退次数 */
    private Long earlyCount;

    /** 缺卡次数 */
    private Long absentCount;
}