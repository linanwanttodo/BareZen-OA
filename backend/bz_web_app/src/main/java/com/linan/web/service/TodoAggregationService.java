package com.linan.web.service;

import com.linan.core.common.api.PageResult;

import java.util.Map;

/**
 * 统一待办聚合服务
 *
 * <p>跨模块只读聚合（请假/加班/出差/用车/用品申领 5 类待我审批事项），
 * 归属 web 编排层而非 Controller：5 类单据 V46 起统一接入审批引擎，
 * 待办以 approval_flow_task 待审节点为唯一数据源，支持数据库分页（非内存分页）。</p>
 *
 * @author BareZen Team
 */
public interface TodoAggregationService {

    /** 待办事项类型 */
    record TodoItem(
            String type,       // leave / overtime / travel / vehicle / supply
            String typeName,   // 类型中文名
            String id,         // 业务记录 ID
            String title,      // 待办标题
            String applicantName,
            String startTime,
            String endTime,
            String createTime) {
    }

    /**
     * 分页聚合待办事项（基于审批引擎待审节点数据库分页）
     *
     * @param userId   当前登录用户（指定审批人节点仅本人可见）
     */
    PageResult<TodoItem> page(Long userId, int pageNum, int pageSize);

    /** 待办分类统计（5 类引擎待审节点数） */
    Map<String, Long> summary(Long userId);
}