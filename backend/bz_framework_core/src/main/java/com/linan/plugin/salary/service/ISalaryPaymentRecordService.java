package com.linan.plugin.salary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.plugin.salary.entity.SalaryPaymentRecord;

import java.util.List;
import java.util.Map;

/**
 * 工资发放记录服务接口
 *
 * @author BareZen Team
 */
public interface ISalaryPaymentRecordService extends IService<SalaryPaymentRecord> {

    /**
     * 分页查询发放记录
     *
     * @param pageNum      页码
     * @param pageSize     每页大小
     * @param paymentMonth 发放月份(YYYY-MM)，可空
     * @param paymentStatus 发放状态，可空
     * @param keyword      员工姓名/工号模糊关键字，可空
     * @return 分页结果
     */
    IPage<SalaryPaymentRecord> pageRecords(long pageNum, long pageSize,
                                           String paymentMonth, String paymentStatus, String keyword);

    /**
     * 按月份从工资结构批量生成发放草稿（幂等：已存在的员工-月份记录跳过）
     *
     * @param paymentMonth 发放月份(YYYY-MM)
     * @param userIds      指定员工，可空表示全部拥有工资结构的员工
     * @return 生成的记录数
     */
    int generateFromStructures(String paymentMonth, List<Long> userIds);

    /**
     * 执行发放（将未发放记录置为已发放）
     *
     * @param ids 发放记录ID列表
     * @return 实际发放条数
     */
    int executePayment(List<Long> ids);

    /**
     * 撤销发放（将已发放记录回退为未发放）
     *
     * @param id 发放记录ID
     */
    void revokePayment(Long id);

    /**
     * 月度工资统计
     *
     * @param paymentMonth 发放月份(YYYY-MM)，可空表示全部月份
     * @return 汇总与按月分布
     */
    Map<String, Object> statistics(String paymentMonth);
}
