package com.linan.web.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端文件公开视图
 *
 * <p>仅暴露列表卡片所需的字段：名称、大小、类型、时间与存储标识；
 * 不返回存储相对路径（filePath）与文件 URL（fileUrl），
 * 避免泄露服务器目录结构，也避免前端绕过下载接口直连静态路径。</p>
 */
@Data
@Schema(name = "UserFileDTO", description = "用户端我的文件")
public class UserFileDTO {

    @Schema(description = "文件ID", example = "1")
    private Long id;

    @Schema(description = "原始文件名", example = "季度总结.pdf")
    private String fileName;

    @Schema(description = "文件大小（字节）", example = "204800")
    private Long fileSize;

    @Schema(description = "文件类型：image/document/video/audio/other", example = "document")
    private String fileType;

    @Schema(description = "存储类型：local/s3", example = "local")
    private String storageType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "上传时间", example = "2026-08-05 10:12:00")
    private LocalDateTime createTime;
}
