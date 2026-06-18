package com.linan.core.common.api;

import com.linan.core.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * PageParamGuard 分页参数校验规则单测
 *
 * <p>锁定两档阈值的边界：业务列表 1..100 与 URL 兜底 1..1000，
 * 以及非法时统一抛 code=400、文案为"分页参数不合法"。</p>
 */
class PageParamGuardTest {

    private void assertRejected(Integer pageNum, Integer pageSize) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PageParamGuard.validate(pageNum, pageSize));
        assertEquals(400, ex.getCode());
        assertEquals(PageParamGuard.INVALID_MESSAGE, ex.getMessage());
    }

    @Nested
    @DisplayName("validate：业务列表 1..100")
    class Validate {

        @Test
        @DisplayName("边界值 1 与 100 合法")
        void acceptsBoundaries() {
            assertDoesNotThrow(() -> PageParamGuard.validate(1, 1));
            assertDoesNotThrow(() -> PageParamGuard.validate(1, PageParamGuard.MAX_PAGE_SIZE));
        }

        @Test
        @DisplayName("101 起超出业务上限")
        void rejectsAboveMax() {
            assertRejected(1, PageParamGuard.MAX_PAGE_SIZE + 1);
        }

        @Test
        @DisplayName("页码与每页条数的非正值一律拒绝")
        void rejectsNonPositive() {
            assertRejected(0, 10);
            assertRejected(-1, 10);
            assertRejected(1, 0);
            assertRejected(1, -10);
        }

        @Test
        @DisplayName("缺任一侧参数即拒绝（Controller 侧两值必有默认值）")
        void rejectsNull() {
            assertRejected(null, 10);
            assertRejected(1, null);
        }
    }

    @Nested
    @DisplayName("validateSuppliedWithinHardCap：URL 兜底 1..1000")
    class HardCap {

        @Test
        @DisplayName("两值皆未传入时放行，交由 @RequestParam 默认值兜底")
        void acceptsWhenBothAbsent() {
            assertDoesNotThrow(() -> PageParamGuard.validateSuppliedWithinHardCap(null, null));
        }

        @Test
        @DisplayName("只传其中一侧时只校验该侧，不因另一侧缺失而误判")
        void acceptsPartialParams() {
            assertDoesNotThrow(() -> PageParamGuard.validateSuppliedWithinHardCap(3, null));
            assertDoesNotThrow(() -> PageParamGuard.validateSuppliedWithinHardCap(null, 500));
        }

        @Test
        @DisplayName("1000 合法，1001 起拒绝（超出即被 SQL 层 maxLimit 静默截断）")
        void capsAtMaxLimit() {
            assertDoesNotThrow(() -> PageParamGuard.validateSuppliedWithinHardCap(1, PageParamGuard.ABSOLUTE_MAX_PAGE_SIZE));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> PageParamGuard.validateSuppliedWithinHardCap(1, PageParamGuard.ABSOLUTE_MAX_PAGE_SIZE + 1));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("业务上限 100 到硬上限 1000 之间放行，保住存量「拉全量」调用")
        void allowsFetchAllRange() {
            assertDoesNotThrow(() -> PageParamGuard.validateSuppliedWithinHardCap(1, 999));
        }

        @Test
        @DisplayName("传入侧的非法值仍被拒绝")
        void rejectsInvalidSupplied() {
            assertThrows(BusinessException.class,
                    () -> PageParamGuard.validateSuppliedWithinHardCap(0, null));
            assertThrows(BusinessException.class,
                    () -> PageParamGuard.validateSuppliedWithinHardCap(null, 0));
        }
    }
}
