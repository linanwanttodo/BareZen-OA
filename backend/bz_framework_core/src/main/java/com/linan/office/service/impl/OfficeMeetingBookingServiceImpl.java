package com.linan.office.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.office.entity.OfficeMeetingBooking;
import com.linan.office.mapper.OfficeMeetingBookingMapper;
import com.linan.office.service.IOfficeMeetingBookingService;
import org.springframework.stereotype.Service;

/**
 * 会议预订服务实现
 *
 * @author BareZen Team
 */
@Service
public class OfficeMeetingBookingServiceImpl extends ServiceImpl<OfficeMeetingBookingMapper, OfficeMeetingBooking>
        implements IOfficeMeetingBookingService {
}
