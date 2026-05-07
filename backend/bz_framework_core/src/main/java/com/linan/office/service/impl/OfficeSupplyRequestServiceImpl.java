package com.linan.office.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.common.exception.BusinessException;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.mapper.OfficeSupplyMapper;
import com.linan.office.mapper.OfficeSupplyRequestMapper;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeSupplyService;
import com.linan.system.service.ApprovalFlowFacade;
import com.linan.system.service.ApprovalNotifyTexts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用品申领记录服务实现
 *
 * <p>审批编排统一收口到 {@link ApprovalFlowFacade}（自审校验 / 引擎节点推进 /
 * 流程日志 / 通知事件 / 事务一致），本服务仅承载单据差异逻辑：
 * 通过时悲观锁扣减库存（同一用品并发扣减串行化，避免超扣）。
 * 任一步骤失败抛出 {@link BusinessException}，库存扣减与状态变更整体回滚。</p>
 *
 * @author BareZen Team
 */
@Service
public class OfficeSupplyRequestServiceImpl extends ServiceImpl<OfficeSupplyRequestMapper, OfficeSupplyRequest>
        implements IOfficeSupplyRequestService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OfficeSupplyMapper supplyMapper;

    private final IOfficeSupplyService supplyService;

    private final ApprovalFlowFacade flowFacade;

    public OfficeSupplyRequestServiceImpl(OfficeSupplyMapper supplyMapper,
                                          IOfficeSupplyService supplyService,
                                          ApprovalFlowFacade flowFacade) {
        this.supplyMapper = supplyMapper;
        this.supplyService = supplyService;
        this.flowFacade = flowFacade;
    }

    /**
     * 审批申领（通过/驳回），通过时扣减库存
     *
     * <p>编排（自审/引擎/日志/通知）由 Facade 统一；单据差异逻辑（悲观锁扣库存 +
     * 业务表原子状态更新 + 文案）在 {@link ApprovalFlowFacade.ApprovalBizHandler} 中。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, String action, String remark) {
        OfficeSupplyRequest request = getById(id);
        if (request == null) {
            throw new BusinessException("申领记录不存在");
        }
        Long approverId = com.linan.core.security.interceptor.AuthInterceptor.getCurrentUserId();
        String approverName = com.linan.core.security.interceptor.AuthInterceptor.getCurrentUsername();

        flowFacade.approve("supply", id, approverId, approverName, action, remark,
                new ApprovalFlowFacade.ApprovalBizHandler() {
                    @Override
                    public Long applicantId() {
                        return request.getUserId();
                    }

                    @Override
                    public boolean finish(String targetStatus, Long approverId, String approverName, String remark) {
                        // 通过时扣减库存（悲观锁锁定用品行，并发串行化避免超扣）
                        if (ApprovalStatus.APPROVED.getValue().equals(targetStatus)) {
                            deductStock(request);
                        }
                        // 业务表原子条件更新（并发兜底：后到者受影响行数为 0）
                        return OfficeSupplyRequestServiceImpl.this.update(
                                new LambdaUpdateWrapper<OfficeSupplyRequest>()
                                        .eq(OfficeSupplyRequest::getId, id)
                                        .eq(OfficeSupplyRequest::getStatus, ApprovalStatus.PENDING.getValue())
                                        .set(OfficeSupplyRequest::getStatus, targetStatus)
                                        .set(OfficeSupplyRequest::getApproverId, approverId)
                                        .set(OfficeSupplyRequest::getApproverName, approverName)
                                        .set(OfficeSupplyRequest::getApproveTime, LocalDateTime.now().format(FMT))
                                        .set(OfficeSupplyRequest::getApproveRemark, remark));
                    }

                    @Override
                    public boolean cancel() {
                        return false;
                    }

                    @Override
                    public ApprovalFlowFacade.NotifyText resultNotify(boolean approved, String remark) {
                        String res = approved ? "已通过" : "已驳回";
                        String content = "您的用品申领（" + request.getSupplyName() + " × " + request.getQuantity() + "）" + res
                                + (remark != null && !remark.isEmpty() ? "，审批意见：" + remark : "");
                        return ApprovalFlowFacade.NotifyText.of(ApprovalNotifyTexts.resultTitle("用品申领", approved), content);
                    }
                });
    }

    /** 悲观锁扣减库存：锁定用品行，库存不足抛业务异常（事务内整体回滚） */
    private void deductStock(OfficeSupplyRequest request) {
        OfficeSupply supply = supplyMapper.selectByIdForUpdate(request.getSupplyId());
        if (supply == null) {
            throw new BusinessException("用品已不存在");
        }
        if (supply.getStock() < request.getQuantity()) {
            throw new BusinessException("库存不足，当前库存 " + supply.getStock());
        }
        supply.setStock(supply.getStock() - request.getQuantity());
        supplyService.updateById(supply);
    }
}