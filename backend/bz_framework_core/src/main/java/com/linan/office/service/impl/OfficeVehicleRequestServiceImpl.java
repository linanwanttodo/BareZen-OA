package com.linan.office.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.mapper.OfficeVehicleRequestMapper;
import com.linan.office.service.IOfficeVehicleRequestService;
import org.springframework.stereotype.Service;

/**
 * 用车申请服务实现
 *
 * @author BareZen Team
 */
@Service
public class OfficeVehicleRequestServiceImpl extends ServiceImpl<OfficeVehicleRequestMapper, OfficeVehicleRequest>
        implements IOfficeVehicleRequestService {
}
