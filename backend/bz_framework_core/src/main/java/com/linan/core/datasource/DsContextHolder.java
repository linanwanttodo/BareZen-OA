package com.linan.core.datasource;

/**
 * 动态数据源上下文持有者
 *
 * <p>使用 ThreadLocal 存储当前线程使用的数据源名称。
 * 每个线程独立维护自己的数据源选择，避免多线程环境下的数据源切换冲突。</p>
 *
 * <p>典型使用流程：</p>
 * <ol>
 *   <li>调用 {@link #set(String)} 设置目标数据源</li>
 *   <li>执行数据库操作（{@link DynamicDataSource} 会自动调用 {@link #get()} 获取当前数据源）</li>
 *   <li>调用 {@link #clear()} 清理上下文（通常在 finally 块中执行）</li>
 * </ol>
 *
 * <p>通常不直接使用此类，而是通过 {@link Ds} 注解 + {@link DsAspect} 切面自动管理。</p>
 */
public class DsContextHolder {

    /**
     * 默认数据源名称
     */
    private static final String MASTER = "master";

    /**
     * 线程本地存储，保存当前线程使用的数据源名称
     */
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程使用的数据源名称
     *
     * @param ds 数据源名称，如果为 null 则使用默认值 "master"
     */
    public static void set(String ds) {
        CONTEXT_HOLDER.set(ds != null ? ds : MASTER);
    }

    /**
     * 获取当前线程使用的数据源名称
     *
     * @return 数据源名称，如果未设置则返回默认值 "master"
     */
    public static String get() {
        String ds = CONTEXT_HOLDER.get();
        return ds != null ? ds : MASTER;
    }

    /**
     * 清理当前线程的数据源上下文
     *
     * <p>必须在每次请求结束后调用（通常在 finally 块中），
     * 防止 ThreadLocal 内存泄漏或后续请求误用之前的数据源设置。</p>
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
