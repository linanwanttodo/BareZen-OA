package com.linan.plugin.masterdata.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.plugin.masterdata.entity.MaterialInfo;

/**
 * 物料信息服务
 *
 * @author BareZen Team
 */
public interface IMaterialInfoService extends IService<MaterialInfo> {

    /**
     * 新增物料（校验编码/名称必填与编码唯一）
     *
     * @return 保存后的物料
     */
    MaterialInfo createMaterial(MaterialInfo material);

    /**
     * 更新物料（校验存在性与编码唯一，排除自身）
     *
     * @return 更新后的物料
     */
    MaterialInfo updateMaterial(MaterialInfo material);
}
