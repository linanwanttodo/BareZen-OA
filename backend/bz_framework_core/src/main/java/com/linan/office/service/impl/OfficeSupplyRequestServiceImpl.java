package com.linan.office.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.mapper.OfficeSupplyRequestMapper;
import com.linan.office.service.IOfficeSupplyRequestService;
import org.springframework.stereotype.Service;

/**
 * 用品申领记录服务实现
 *
 * @author BareZen Team
 */
@Service
public class OfficeSupplyRequestServiceImpl extends ServiceImpl<OfficeSupplyRequestMapper, OfficeSupplyRequest>
        implements IOfficeSupplyRequestService {
}
