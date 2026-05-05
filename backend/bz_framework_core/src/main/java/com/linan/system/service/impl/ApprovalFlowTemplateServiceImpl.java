package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.ApprovalFlowTemplate;
import com.linan.system.mapper.ApprovalFlowTemplateMapper;
import com.linan.system.service.IApprovalFlowTemplateService;
import org.springframework.stereotype.Service;

/**
 * 审批流程模板服务实现
 *
 * @author BareZen Team
 */
@Service
public class ApprovalFlowTemplateServiceImpl
        extends ServiceImpl<ApprovalFlowTemplateMapper, ApprovalFlowTemplate>
        implements IApprovalFlowTemplateService {
}