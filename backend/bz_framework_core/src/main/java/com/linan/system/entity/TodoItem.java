package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 待办事项实体，对应 todo_item 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("todo_item")
public class TodoItem extends BaseEntity {

    /** 待办标题 */
    private String title;

    /** 待办描述 */
    private String description;

    /** 所属用户ID */
    private Long userId;

    /** 优先级：0=低 1=中 2=高 3=紧急 */
    private Integer priority;

    /** 状态：0=待办 1=进行中 2=已完成 3=已取消 */
    private Integer status;

    /** 截止日期 */
    private String dueDate;

    /** 完成时间 */
    private String completedTime;
}
