package com.linan.plugin.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.plugin.masterdata.entity.SupplierInfo;
import com.linan.plugin.masterdata.mapper.SupplierInfoMapper;
import com.linan.plugin.masterdata.service.ISupplierInfoService;
import org.springframework.stereotype.Service;

/**
 * 供应商信息服务实现
 *
 * @author BareZen Team
 */
@Service
public class SupplierInfoServiceImpl extends ServiceImpl<SupplierInfoMapper, SupplierInfo>
        implements ISupplierInfoService {

    @Override
    public SupplierInfo createSupplier(SupplierInfo supplier) {
        validate(supplier);
        if (existsByCode(supplier.getSupplierCode(), null)) {
            throw new BusinessException("供应商编码已存在: " + supplier.getSupplierCode());
        }
        if (supplier.getStatus() == null || supplier.getStatus().isBlank()) {
            supplier.setStatus("0");
        }
        save(supplier);
        return supplier;
    }

    @Override
    public SupplierInfo updateSupplier(SupplierInfo supplier) {
        if (supplier.getId() == null) {
            throw new BusinessException("供应商ID不能为空");
        }
        if (getById(supplier.getId()) == null) {
            throw new BusinessException("供应商不存在");
        }
        validate(supplier);
        if (existsByCode(supplier.getSupplierCode(), supplier.getId())) {
            throw new BusinessException("供应商编码已存在: " + supplier.getSupplierCode());
        }
        updateById(supplier);
        return supplier;
    }

    private void validate(SupplierInfo supplier) {
        if (supplier.getSupplierCode() == null || supplier.getSupplierCode().isBlank()) {
            throw new BusinessException("供应商编码不能为空");
        }
        if (supplier.getSupplierName() == null || supplier.getSupplierName().isBlank()) {
            throw new BusinessException("供应商名称不能为空");
        }
    }

    private boolean existsByCode(String code, Long excludeId) {
        return count(new LambdaQueryWrapper<SupplierInfo>()
                .eq(SupplierInfo::getSupplierCode, code)
                .ne(excludeId != null, SupplierInfo::getId, excludeId)) > 0;
    }
}
