package com.linan.office.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会议室实体，对应 office_meeting_room 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_meeting_room")
public class OfficeMeetingRoom extends BaseEntity {

    /** 会议室名称 */
    private String roomName;

    /** 容纳人数 */
    private Integer capacity;

    /** 位置 */
    private String location;

    /** 设备（投影/白板/视频会议，逗号分隔） */
    private String equipment;

    /** 状态：0=可用 1=停用 */
    private String status;

    /** 备注 */
    private String remark;
}
