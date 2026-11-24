package com.linan.plugin.masterdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单据号规则实体
 *
 * <p>按 biz_key 定义前缀、日期段格式与序号步长，由
 * {@link com.linan.plugin.masterdata.service.IDocNumberService} 行锁发号，
 * 供采购/销售等后续模块复用。</p>
 *
 * @author BareZen Team
 */
@Data
@TableName("sys_doc_number_rule")
@Schema(description = "单据号规则")
public class DocNumberRule {

    @TableId(type = IdType.AUTO)
    @Schema(description = "规则ID")
    private Long id;

    @Schema(description = "业务键（供应商SUP/客户CUS/物料MAT等）")
    private String bizKey;

    @Schema(description = "单据号前缀")
    private String prefix;

    @Schema(description = "日期段格式（DateTimeFormatter 模式）")
    private String dateFormat;

    @Schema(description = "当前序号（该日期段内已发最大序号）")
    private Long currentSeq;

    @Schema(description = "步长")
    private Integer step;

    @Schema(description = "最近发号日期串（跨日重置序号的依据）")
    private String lastDate;

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
     * 规则为物理删除契约（V58，风格对齐 salary V56），del_flag 仅作历史列保留。
     */
    @Schema(description = "删除标记")
    @JsonIgnore
    private Integer delFlag;
}
