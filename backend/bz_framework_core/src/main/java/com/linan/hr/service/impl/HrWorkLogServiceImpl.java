package com.linan.hr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.hr.entity.HrWorkLog;
import com.linan.hr.mapper.HrWorkLogMapper;
import com.linan.hr.service.IHrWorkLogService;
import org.springframework.stereotype.Service;

/**
 * 工作日志服务实现
 *
 * @author BareZen Team
 */
@Service
public class HrWorkLogServiceImpl extends ServiceImpl<HrWorkLogMapper, HrWorkLog>
        implements IHrWorkLogService {
}
