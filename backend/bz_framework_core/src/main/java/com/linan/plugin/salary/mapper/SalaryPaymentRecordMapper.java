package com.linan.plugin.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.plugin.salary.entity.SalaryPaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工资发放记录Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface SalaryPaymentRecordMapper extends BaseMapper<SalaryPaymentRecord> {
}
