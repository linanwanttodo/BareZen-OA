package com.linan.office.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.office.entity.OfficeMeetingRoom;
import com.linan.office.mapper.OfficeMeetingRoomMapper;
import com.linan.office.service.IOfficeMeetingRoomService;
import org.springframework.stereotype.Service;

/**
 * 会议室服务实现
 *
 * @author BareZen Team
 */
@Service
public class OfficeMeetingRoomServiceImpl extends ServiceImpl<OfficeMeetingRoomMapper, OfficeMeetingRoom>
        implements IOfficeMeetingRoomService {
}
