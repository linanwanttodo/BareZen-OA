package com.linan.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.hr.entity.HrEmployee;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工档案 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface HrEmployeeMapper extends BaseMapper<HrEmployee> {
}
