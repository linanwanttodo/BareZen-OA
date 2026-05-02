package com.linan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.system.entity.ApprovalFlowLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批流程历史 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface ApprovalFlowLogMapper extends BaseMapper<ApprovalFlowLog> {
}
