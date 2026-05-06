package com.linan.system.service;

import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.common.exception.BusinessException;
import com.linan.system.entity.ApprovalFlowTask;

import java.util.List;

/**
 * 审批流程编排门面
 *
 * <p>将审批单据的「提交 / 审批 / 撤销」骨架统一收口（单一职责来源），
 * 业务侧通过 {@link ApprovalBizHandler} 回调注入"该单据的原子状态更新与通知文案"，
 * 避免请假/加班/出差/用车/用品 多类单据在各自 Controller 中重复编排
 * （引擎推进 → 终态更新 → 流程日志 → 通知事件）。逻辑串行点统一加事务，保证
 * 业务表、审批节点、流程日志三者原子一致。</p>
 *
 * <p>骨架职责（本门面统一实现）：</p>
 * <ul>
 *   <li>自审 / 归属校验（approverId 不得等于申请人）</li>
 *   <li>引擎节点推进、中间节点催办下一审批人</li>
 *   <li>终态业务表原子更新（handler.finish）+ 并发兜底</li>
 *   <li>审批流程日志（通过/驳回/撤销）</li>
 *   <li>结果 / 催办通知事件发布</li>
 * </ul>
 *
 * @author BareZen Team
 */
public interface ApprovalFlowFacade {

    /**
     * 提交：启动引擎首节点 + 记录「提交申请」日志（同一事务内）
     *
     * @param bizType      业务类型（leave/overtime/travel/vehicle）
     * @param bizId        业务记录ID
     * @param assigneeId   指定审批人（可空）
     * @param assigneeName 指定审批人姓名（可空）
     * @param operatorId   申请人ID
     * @param operatorName 申请人姓名
     */
    void submit(String bizType, Long bizId, Long assigneeId, String assigneeName,
                Long operatorId, String operatorName);

    /**
     * 撤销申请（仅待审批）：业务表 0→3 + 引擎节点取消 + 撤销日志（同一事务内）
     *
     * @throws BusinessException 非本人撤销 / 不处于待审批（含流程日志留痕）
     */
    void revoke(String bizType, Long bizId, Long operatorId, String operatorName,
                ApprovalBizHandler handler);

    /**
     * 审批（通过/驳回）：自审校验 → 引擎推进 → 终态更新 + 日志 + 通知（同一事务内）
     *
     * @throws BusinessException 自审 / 无待审节点 / 重复审批 / 终态更新失败
     */
    void approve(String bizType, Long bizId, Long approverId, String approverName,
                 String action, String remark, ApprovalBizHandler handler);

    /**
     * 业务单据审批回调
     *
     * <p>由各单据（controller/应用层）实现，承载与业务表强相关的差异逻辑
     * （归属字段、原子状态更新、通知文案），保持门面与引擎层不依赖具体业务实体。</p>
     */
    interface ApprovalBizHandler {

        /** 申请人用户ID（自审校验与结果通知对象） */
        Long applicantId();

        /**
         * 终态原子更新：业务表 status 0→targetStatus，记录审批人/时间/意见
         *
         * @return true=生效；false=并发已处理（未更新）
         */
        default boolean finish(String targetStatus, Long approverId, String approverName, String remark) {
            return false;
        }

        /** 撤销原子更新：业务表 status 0→3，true=生效 */
        boolean cancel();

        /** 单据当前状态（用于撤销失败留痕，回显实际状态以便审计） */
        default String currentStatus() {
            return ApprovalStatus.PENDING.getValue();
        }

        /** 结果通知文案 */
        default NotifyText resultNotify(boolean approved, String remark) {
            return NotifyText.of("", "");
        }

        /** 下一审批人催办文案 */
        default NotifyText nextNotify() {
            return NotifyText.of("", "");
        }
    }

    /** 通知文案载体 */
    record NotifyText(String title, String content) {
        public static NotifyText of(String title, String content) {
            return new NotifyText(title, content);
        }
    }
}