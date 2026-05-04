package com.linan.office.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.event.ApprovalCompletedEvent;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.mapper.OfficeSupplyMapper;
import com.linan.office.mapper.OfficeSupplyRequestMapper;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeSupplyService;
import com.linan.system.service.IApprovalFlowLogService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用品申领记录服务实现
 *
 * @author BareZen Team
 */
@Service
public class OfficeSupplyRequestServiceImpl extends ServiceImpl<OfficeSupplyRequestMapper, OfficeSupplyRequest>
        implements IOfficeSupplyRequestService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OfficeSupplyMapper supplyMapper;

    private final IOfficeSupplyService supplyService;

    private final IApprovalFlowLogService flowLogService;

    private final ApplicationEventPublisher eventPublisher;

    public OfficeSupplyRequestServiceImpl(OfficeSupplyMapper supplyMapper,
                                          IOfficeSupplyService supplyService,
                                          IApprovalFlowLogService flowLogService,
                                          ApplicationEventPublisher eventPublisher) {
        this.supplyMapper = supplyMapper;
        this.supplyService = supplyService;
        this.flowLogService = flowLogService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 审批申领（通过/驳回），通过时扣减库存
     *
     * <p>事务内悲观锁：先锁定申领记录行（同一记录的并发审批串行化，
     * 后到者等待锁释放后可见最新状态），通过时再锁定用品行
     * （同一用品的并发扣减串行化，避免库存超扣）。
     * 任一步骤失败抛出 {@link BusinessException}，库存扣减与状态变更整体回滚。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, String action, String remark) {
        // 悲观锁锁定申领记录行，使同一记录的并发审批串行化
        OfficeSupplyRequest request = baseMapper.selectByIdForUpdate(id);
        if (request == null) {
            throw new BusinessException("申领记录不存在");
        }
        if (!"0".equals(request.getStatus())) {
            throw new BusinessException("该记录已审批，不能重复操作");
        }
        Long approverId = AuthInterceptor.getCurrentUserId();
        if (approverId != null && approverId.equals(request.getUserId())) {
            throw new BusinessException("不能审批自己的申请");
        }
        // 指定审批人校验（V31）：若申请指定了审批人，仅该审批人可操作
        if (request.getAssigneeId() != null
                && (approverId == null || !approverId.equals(request.getAssigneeId()))) {
            throw new BusinessException("该申请已指定审批人（" + request.getAssigneeName() + "），仅其可审批");
        }

        if ("pass".equals(action)) {
            // 悲观锁锁定用品行，使同一用品的并发扣减串行化，避免库存超扣
            OfficeSupply supply = supplyMapper.selectByIdForUpdate(request.getSupplyId());
            if (supply == null) {
                throw new BusinessException("用品已不存在");
            }
            if (supply.getStock() < request.getQuantity()) {
                throw new BusinessException("库存不足，当前库存 " + supply.getStock());
            }
            supply.setStock(supply.getStock() - request.getQuantity());
            supplyService.updateById(supply);
            request.setStatus("1");
        } else if ("reject".equals(action)) {
            request.setStatus("2");
        } else {
            throw new BusinessException("无效的审批动作");
        }

        request.setApproverId(approverId);
        request.setApproverName(AuthInterceptor.getCurrentUsername());
        request.setApproveTime(LocalDateTime.now().format(FMT));
        request.setApproveRemark(remark);
        if (!updateById(request)) {
            throw new BusinessException("审批失败");
        }

        String node = "pass".equals(action) ? "审批通过" : "审批驳回";
        flowLogService.record("supply", request.getId(), node, request.getStatus(),
                approverId, AuthInterceptor.getCurrentUsername(), remark);
        String result = "pass".equals(action) ? "已通过" : "已驳回";
        eventPublisher.publishEvent(new ApprovalCompletedEvent(
                request.getUserId(),
                "用品申领审批" + result,
                "您的用品申领（" + request.getSupplyName() + " × " + request.getQuantity() + "）" + result
                        + (remark != null && !remark.isEmpty() ? "，审批意见：" + remark : "")));
    }
}
