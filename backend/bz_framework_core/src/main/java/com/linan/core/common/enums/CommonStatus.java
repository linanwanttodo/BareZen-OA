package com.linan.core.common.enums;

/**
 * 通用状态枚举
 *
 * <p>用于替代 {@code "0"}, {@code "1"} 等魔法字符串。</p>
 */
public enum CommonStatus {

    /** 正常 */
    NORMAL("0"),
    /** 停用/禁用 */
    DISABLED("1"),
    /** 已删除 */
    DELETED("1");

    private final String value;

    CommonStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean matches(String value) {
        return this.value.equals(value);
    }
}
