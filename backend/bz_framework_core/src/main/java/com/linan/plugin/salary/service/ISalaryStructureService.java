package com.linan.plugin.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.plugin.salary.entity.SalaryStructure;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工资结构服务接口
 *
 * @author BareZen Team
 */
public interface ISalaryStructureService extends IService<SalaryStructure> {

    /**
     * 计算员工实发工资
     *
     * @param userId 员工ID
     * @return 实发工资
     */
    BigDecimal calculateActualSalary(Long userId);

    /**
     * 批量计算实发工资
     *
     * @param userIds 员工ID列表
     * @return 工资结构列表（包含计算后的实发工资）
     */
    List<SalaryStructure> batchCalculateActualSalary(List<Long> userIds);

    /**
     * 查询员工工资结构
     *
     * @param userId 员工ID
     * @return 工资结构
     */
    SalaryStructure getByUserId(Long userId);
}