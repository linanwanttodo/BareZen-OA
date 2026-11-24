package com.linan.plugin.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.plugin.salary.entity.SalaryStructure;
import com.linan.plugin.salary.mapper.SalaryStructureMapper;
import com.linan.plugin.salary.service.ISalaryStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工资结构服务实现
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class SalaryStructureServiceImpl extends ServiceImpl<SalaryStructureMapper, SalaryStructure>
        implements ISalaryStructureService {

    @Override
    public BigDecimal calculateActualSalary(Long userId) {
        SalaryStructure structure = getByUserId(userId);
        if (structure == null) {
            throw new BusinessException("员工工资结构不存在");
        }

        // 实发工资 = 基本工资 + 绩效工资 + 各项补贴 - 社保 - 公积金 - 个税
        BigDecimal totalIncome = structure.getBaseSalary()
                .add(structure.getPerformanceSalary())
                .add(structure.getPositionAllowance())
                .add(structure.getTransportAllowance())
                .add(structure.getMealAllowance())
                .add(structure.getOtherAllowance());

        BigDecimal totalDeduction = structure.getSocialInsurancePersonal()
                .add(structure.getHousingFundPersonal())
                .add(structure.getIncomeTax());

        return totalIncome.subtract(totalDeduction);
    }

    @Override
    public List<SalaryStructure> batchCalculateActualSalary(List<Long> userIds) {
        return list(new LambdaQueryWrapper<SalaryStructure>()
                .in(SalaryStructure::getUserId, userIds));
    }

    @Override
    public SalaryStructure getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<SalaryStructure>()
                .eq(SalaryStructure::getUserId, userId));
    }
}