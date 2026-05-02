package com.linan.office.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会议预订实体，对应 office_meeting_booking 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_meeting_booking")
public class OfficeMeetingBooking extends BaseEntity {

    /** 会议室ID */
    private Long roomId;

    /** 会议室名称（冗余） */
    private String roomName;

    /** 会议主题 */
    private String title;

    /** 预订人用户ID */
    private Long bookerId;

    /** 预订人姓名 */
    private String bookerName;

    /** 开始时间（yyyy-MM-dd HH:mm） */
    private String startTime;

    /** 结束时间（yyyy-MM-dd HH:mm） */
    private String endTime;

    /** 参会人员（逗号分隔） */
    private String attendees;

    /** 备注 */
    private String remark;
}
