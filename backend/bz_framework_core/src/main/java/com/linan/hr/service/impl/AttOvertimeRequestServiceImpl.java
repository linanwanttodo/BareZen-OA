package com.linan.hr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.mapper.AttOvertimeRequestMapper;
import com.linan.hr.service.IAttOvertimeRequestService;
import org.springframework.stereotype.Service;

/**
 * 加班申请服务实现
 *
 * @author BareZen Team
 */
@Service
public class AttOvertimeRequestServiceImpl extends ServiceImpl<AttOvertimeRequestMapper, AttOvertimeRequest>
        implements IAttOvertimeRequestService {
}
