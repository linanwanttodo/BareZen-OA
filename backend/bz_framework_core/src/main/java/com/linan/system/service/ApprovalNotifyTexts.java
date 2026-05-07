package com.linan.system.service;

/**
 * 审批通知文案构造工具
 *
 * <p>收敛 5 类单据匿名 {@code ApprovalBizHandler} 中重复的「结果通知标题」「催办通知标题」
 * 拼装，统一「{业务名}审批{已通过|已驳回}」「新的待审批申请」格式，避免散落拷贝导致的漂移。</p>
 */
public final class ApprovalNotifyTexts {

    private ApprovalNotifyTexts() {
    }

    /** 结果通知标题：{业务名}审批{已通过|已驳回} */
    public static String resultTitle(String bizName, boolean approved) {
        return bizName + "审批" + (approved ? "已通过" : "已驳回");
    }

    /** 催办通知标题 */
    public static String nextNotifyTitle() {
        return "新的待审批申请";
    }
}