package com.linan.hr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.mapper.AttTravelRequestMapper;
import com.linan.hr.service.IAttTravelRequestService;
import org.springframework.stereotype.Service;

/**
 * 出差申请服务实现
 *
 * @author BareZen Team
 */
@Service
public class AttTravelRequestServiceImpl extends ServiceImpl<AttTravelRequestMapper, AttTravelRequest>
        implements IAttTravelRequestService {
}
