package com.linan.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工档案实体，对应 hr_employee 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee")
public class HrEmployee extends BaseEntity {

    /** 员工工号（唯一） */
    private String empNo;

    /** 姓名 */
    private String name;

    /** 性别：0=男 1=女 2=未知 */
    private String gender;

    /** 所属部门ID */
    private Long deptId;

    /** 所属部门名称（冗余，便于列表展示） */
    private String deptName;

    /** 职位 */
    private String position;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 身份证号（脱敏存储） */
    private String idCard;

    /** 入职日期 */
    private String hireDate;

    /** 离职日期 */
    private String leaveDate;

    /** 用工类型：0=全职 1=实习 2=兼职 3=劳务 */
    private String employmentType;

    /** 学历 */
    private String education;

    /** 状态：0=试用 1=在职 2=离职 */
    private String status;

    /** 备注 */
    private String remark;
}
