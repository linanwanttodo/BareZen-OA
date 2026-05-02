package com.linan.office.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.mapper.OfficeSupplyMapper;
import com.linan.office.service.IOfficeSupplyService;
import org.springframework.stereotype.Service;

/**
 * 办公用品服务实现
 *
 * @author BareZen Team
 */
@Service
public class OfficeSupplyServiceImpl extends ServiceImpl<OfficeSupplyMapper, OfficeSupply>
        implements IOfficeSupplyService {
}
