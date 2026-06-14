package com.linan.plugin.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工资发放记录实体
 *
 * 对应 V47 中的 salary_payment_record 表，按（员工，月份）唯一。
 *
 * @author BareZen Team
 */
@Data
@TableName("salary_payment_record")
@Schema(description = "工资发放记录")
public class SalaryPaymentRecord {

    @TableId(type = IdType.AUTO)
    @Schema(description = "发放记录ID")
    private Long id;

    @Schema(description = "员工ID")
    private Long userId;

    @Schema(description = "员工姓名")
    private String userName;

    @Schema(description = "发放月份(YYYY-MM)")
    private String paymentMonth;

    @Schema(description = "基本工资")
    private BigDecimal baseSalary;

    @Schema(description = "绩效工资")
    private BigDecimal performanceSalary;

    @Schema(description = "岗位津贴")
    private BigDecimal positionAllowance;

    @Schema(description = "交通补贴")
    private BigDecimal transportAllowance;

    @Schema(description = "餐补")
    private BigDecimal mealAllowance;

    @Schema(description = "其他补贴")
    private BigDecimal otherAllowance;

    @Schema(description = "总收入")
    private BigDecimal totalIncome;

    @Schema(description = "社保个人缴纳")
    private BigDecimal socialInsurancePersonal;

    @Schema(description = "公积金个人缴纳")
    private BigDecimal housingFundPersonal;

    @Schema(description = "个税")
    private BigDecimal incomeTax;

    @Schema(description = "总扣除")
    private BigDecimal totalDeduction;

    @Schema(description = "实发工资")
    private BigDecimal actualSalary;

    @Schema(description = "发放状态: 0=未发放, 1=已发放")
    private String paymentStatus;

    @Schema(description = "发放时间")
    private LocalDateTime paymentTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private Long createBy;

    @Schema(description = "更新人")
    private Long updateBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    @Schema(description = "删除标记")
    private Integer delFlag;
}
