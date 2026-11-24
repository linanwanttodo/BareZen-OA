package com.linan.plugin.masterdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.plugin.masterdata.entity.CustomerInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户信息 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface CustomerInfoMapper extends BaseMapper<CustomerInfo> {
}
