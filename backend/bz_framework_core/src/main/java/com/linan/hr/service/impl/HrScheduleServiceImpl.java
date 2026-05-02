package com.linan.hr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.hr.entity.HrSchedule;
import com.linan.hr.mapper.HrScheduleMapper;
import com.linan.hr.service.IHrScheduleService;
import org.springframework.stereotype.Service;

/**
 * 个人日程服务实现
 *
 * @author BareZen Team
 */
@Service
public class HrScheduleServiceImpl extends ServiceImpl<HrScheduleMapper, HrSchedule>
        implements IHrScheduleService {
}
