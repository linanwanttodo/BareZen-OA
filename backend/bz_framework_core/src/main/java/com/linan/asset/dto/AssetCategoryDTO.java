package com.linan.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 资产分类数据传输对象
 *
 * 用于API请求和响应，避免直接暴露Entity对象
 *
 * @author BareZen Team
 */
@Data
@Schema(description = "资产分类数据传输对象")
public class AssetCategoryDTO {

    @Schema(description = "分类ID", example = "1")
    private Long id;

    @Schema(description = "分类名称", example = "办公设备", required = true)
    @NotBlank(message = "分类名称不能为空")
    private String name;

    @Schema(description = "父分类ID（0=根分类）", example = "0")
    private Long parentId;

    @Schema(description = "排序号", example = "1")
    private Integer sort;

    @Schema(description = "是否启用：1=启用 0=停用", example = "1", allowableValues = {"0", "1"})
    private Integer status;
}