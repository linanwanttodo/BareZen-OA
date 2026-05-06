package com.linan.core.common.enums;

import java.util.Arrays;

/**
 * 审批状态枚举
 *
 * <p>统一审批单据（请假/加班/出差/用车/用品申领）业务表 status 的取值，
 * 替代散落的魔法字符串 {@code "0"/"1"/"2"/"3"}。与审批引擎节点状态一致。
 */
public enum ApprovalStatus {

    /** 待审批 */
    PENDING("0"),
    /** 已通过 */
    APPROVED("1"),
    /** 已驳回 */
    REJECTED("2"),
    /** 已撤销 */
    CANCELED("3");

    private final String value;

    ApprovalStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean matches(String value) {
        return this.value.equals(value);
    }

    /** 按字符串取枚举，未匹配返回 null */
    public static ApprovalStatus fromCode(String value) {
        return Arrays.stream(values())
                .filter(s -> s.value.equals(value))
                .findFirst().orElse(null);
    }
}