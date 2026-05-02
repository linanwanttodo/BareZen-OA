package com.linan.hr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.mapper.AttLeaveRequestMapper;
import com.linan.hr.service.IAttLeaveRequestService;
import org.springframework.stereotype.Service;

/**
 * 请假申请服务实现
 *
 * @author BareZen Team
 */
@Service
public class AttLeaveRequestServiceImpl extends ServiceImpl<AttLeaveRequestMapper, AttLeaveRequest>
        implements IAttLeaveRequestService {
}
