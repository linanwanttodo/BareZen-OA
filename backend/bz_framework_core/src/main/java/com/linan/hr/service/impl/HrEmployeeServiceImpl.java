package com.linan.hr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.hr.entity.HrEmployee;
import com.linan.hr.mapper.HrEmployeeMapper;
import com.linan.hr.service.IHrEmployeeService;
import org.springframework.stereotype.Service;

/**
 * 员工档案服务实现
 *
 * @author BareZen Team
 */
@Service
public class HrEmployeeServiceImpl extends ServiceImpl<HrEmployeeMapper, HrEmployee>
        implements IHrEmployeeService {
}
