package com.linan.plugin.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工资结构实体
 *
 * 定义员工的基本工资、绩效工资、津贴补贴等工资组成部分
 *
 * @author BareZen Team
 */
@Data
@TableName("salary_structure")
@Schema(description = "工资结构")
public class SalaryStructure {

    @TableId(type = IdType.AUTO)
    @Schema(description = "工资结构ID")
    private Long id;

    @Schema(description = "员工ID")
    private Long userId;

    @Schema(description = "员工姓名")
    private String userName;

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

    @Schema(description = "社保个人缴纳")
    private BigDecimal socialInsurancePersonal;

    @Schema(description = "公积金个人缴纳")
    private BigDecimal housingFundPersonal;

    @Schema(description = "个税")
    private BigDecimal incomeTax;

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

    /**
     * 配置数据无审计价值，本表为物理删除（V56 起契约），del_flag 仅作历史列保留。
     * 不标 @TableLogic：标了会把 removeById 变成软删，user_id 永久占用 uk_user_id，员工无法重建工资结构。
     */
    @Schema(description = "删除标记")
    @JsonIgnore
    private Integer delFlag;
}