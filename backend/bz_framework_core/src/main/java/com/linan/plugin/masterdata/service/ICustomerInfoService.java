package com.linan.plugin.masterdata.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.plugin.masterdata.entity.CustomerInfo;

/**
 * 客户信息服务
 *
 * @author BareZen Team
 */
public interface ICustomerInfoService extends IService<CustomerInfo> {

    /**
     * 新增客户（校验编码/名称必填与编码唯一）
     *
     * @return 保存后的客户
     */
    CustomerInfo createCustomer(CustomerInfo customer);

    /**
     * 更新客户（校验存在性与编码唯一，排除自身）
     *
     * @return 更新后的客户
     */
    CustomerInfo updateCustomer(CustomerInfo customer);
}
