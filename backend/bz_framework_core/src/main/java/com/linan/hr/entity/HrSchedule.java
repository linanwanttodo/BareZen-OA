package com.linan.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 个人日程实体，对应 hr_schedule 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_schedule")
public class HrSchedule extends BaseEntity {

    /** 所属用户ID */
    private Long userId;

    /** 所属用户姓名（冗余） */
    private String userName;

    /** 日程标题 */
    @NotBlank(message = "日程标题不能为空")
    private String title;

    /** 日程描述 */
    private String description;

    /** 开始时间（yyyy-MM-dd HH:mm） */
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    /** 结束时间（yyyy-MM-dd HH:mm） */
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    /** 提醒时间（yyyy-MM-dd HH:mm，null=不提醒） */
    private String remindTime;

    /** 状态：0=未开始 1=进行中 2=已完成 3=已取消 */
    private String status;

    /** 地点 */
    private String location;
}
