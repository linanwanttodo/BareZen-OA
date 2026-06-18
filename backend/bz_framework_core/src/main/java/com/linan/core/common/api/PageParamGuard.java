package com.linan.core.common.api;

import com.linan.core.common.exception.BusinessException;

/**
 * 分页参数统一校验
 *
 * <p>此前同一条 {@code 1..100} 规则在 13 处手写展开，另有 {@code isValidPageParams}
 * 助手在 {@code BaseController} 与 {@code SysRoleController} 各存一份拷贝，
 * 37 个带分页参数的 Controller 中约 23 个完全没有校验。此处的唯一实现供 Controller
 * 主动调用，并由 {@code PageParamInterceptor} 作为未调用时的兜底防线。</p>
 */
public final class PageParamGuard {

    /** 非法分页参数的统一提示，Controller 与拦截器共用 */
    public static final String INVALID_MESSAGE = "分页参数不合法";

    /** 业务列表页每页上限，与前端分页器可选的最大每页条数一致 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 每页硬上限，与 MyBatisPlusConfig 中 PaginationInnerInterceptor.maxLimit 对齐 */
    public static final int ABSOLUTE_MAX_PAGE_SIZE = 1000;

    private PageParamGuard() {
    }

    /**
     * 校验业务列表分页参数，两值都必须存在且落在 {@code 1..}{@value #MAX_PAGE_SIZE} 内。
     *
     * @throws BusinessException code=400，当页码或每页条数缺失、越界
     */
    public static void validate(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageSize == null) {
            invalid();
        }
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            invalid();
        }
    }

    /**
     * 校验 URL 上实际传入的分页参数不超过 SQL 层硬上限；未传入的一侧交由各自的
     * {@code @RequestParam} 默认值兜底，不在此处判非法。
     *
     * <p>阈值取硬上限而非业务上限 100，是为了不打断存量"一次拉全量"调用；同时超出
     * {@code maxLimit} 的请求本会被 SQL 层静默截断，让调用方误以为拿到全量，
     * 因此这里把它变成可见错误。</p>
     *
     * @throws BusinessException code=400，当页码或每页条数越界
     */
    public static void validateSuppliedWithinHardCap(Integer pageNum, Integer pageSize) {
        if (pageNum != null && pageNum < 1) {
            invalid();
        }
        if (pageSize != null && (pageSize < 1 || pageSize > ABSOLUTE_MAX_PAGE_SIZE)) {
            invalid();
        }
    }

    private static void invalid() {
        throw new BusinessException(ResultCodeEnum.VALIDATE_FAILED, INVALID_MESSAGE);
    }
}
