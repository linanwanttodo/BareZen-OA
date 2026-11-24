package com.linan.plugin.masterdata.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.plugin.masterdata.entity.SupplierInfo;

/**
 * 供应商信息服务
 *
 * @author BareZen Team
 */
public interface ISupplierInfoService extends IService<SupplierInfo> {

    /**
     * 新增供应商（校验编码/名称必填与编码唯一）
     *
     * @return 保存后的供应商
     */
    SupplierInfo createSupplier(SupplierInfo supplier);

    /**
     * 更新供应商（校验存在性与编码唯一，排除自身）
     *
     * @return 更新后的供应商
     */
    SupplierInfo updateSupplier(SupplierInfo supplier);
}
