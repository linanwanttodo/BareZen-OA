package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文件存储实体，对应 sys_file（支持文件夹+可见范围） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {
    private String fileName;
    private String originalName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String storageType;

    /** 是否文件夹（0=文件，1=文件夹） */
    private Integer isFolder;

    /** 父文件夹ID（0=根目录） */
    private Long folderId;

    /** 可见范围（admin=管理端可见，public=所有用户可见） */
    private String scope;

    /** 排序号 */
    private Integer sortOrder;
}
