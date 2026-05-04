package com.linan.office.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.office.entity.OfficeSupplyRequest;

/**
 * 用品申领记录服务接口
 *
 * @author BareZen Team
 */
public interface IOfficeSupplyRequestService extends IService<OfficeSupplyRequest> {

    /**
     * 审批申领（通过/驳回），通过时扣减库存
     *
     * <p>事务内悲观锁：先锁定申领记录行，通过时再锁定用品行，避免重复审批与库存超扣。
     * 业务失败抛出 {@link com.linan.core.common.exception.BusinessException}，
     * 由全局异常处理器统一返回 Result，事务自动回滚。</p>
     *
     * @param id     申领记录 id
     * @param action pass=通过 / reject=驳回
     * @param remark 审批意见
     */
    void approve(Long id, String action, String remark);
}
