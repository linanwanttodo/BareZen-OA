package com.linan.plugin.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.plugin.masterdata.entity.CustomerInfo;
import com.linan.plugin.masterdata.mapper.CustomerInfoMapper;
import com.linan.plugin.masterdata.service.ICustomerInfoService;
import org.springframework.stereotype.Service;

/**
 * 客户信息服务实现
 *
 * @author BareZen Team
 */
@Service
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo>
        implements ICustomerInfoService {

    @Override
    public CustomerInfo createCustomer(CustomerInfo customer) {
        validate(customer);
        if (existsByCode(customer.getCustomerCode(), null)) {
            throw new BusinessException("客户编码已存在: " + customer.getCustomerCode());
        }
        if (customer.getStatus() == null || customer.getStatus().isBlank()) {
            customer.setStatus("0");
        }
        save(customer);
        return customer;
    }

    @Override
    public CustomerInfo updateCustomer(CustomerInfo customer) {
        if (customer.getId() == null) {
            throw new BusinessException("客户ID不能为空");
        }
        if (getById(customer.getId()) == null) {
            throw new BusinessException("客户不存在");
        }
        validate(customer);
        if (existsByCode(customer.getCustomerCode(), customer.getId())) {
            throw new BusinessException("客户编码已存在: " + customer.getCustomerCode());
        }
        updateById(customer);
        return customer;
    }

    private void validate(CustomerInfo customer) {
        if (customer.getCustomerCode() == null || customer.getCustomerCode().isBlank()) {
            throw new BusinessException("客户编码不能为空");
        }
        if (customer.getCustomerName() == null || customer.getCustomerName().isBlank()) {
            throw new BusinessException("客户名称不能为空");
        }
    }

    private boolean existsByCode(String code, Long excludeId) {
        return count(new LambdaQueryWrapper<CustomerInfo>()
                .eq(CustomerInfo::getCustomerCode, code)
                .ne(excludeId != null, CustomerInfo::getId, excludeId)) > 0;
    }
}
