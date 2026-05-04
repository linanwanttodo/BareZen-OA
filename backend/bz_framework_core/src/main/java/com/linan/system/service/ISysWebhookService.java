package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysWebhook;

import java.util.List;
import java.util.Map;

/**
 * Webhook 事件订阅服务接口
 *
 * @author BareZen Team
 */
public interface ISysWebhookService extends IService<SysWebhook> {

    /**
     * 异步推送事件到所有订阅了该事件的启用 Webhook
     *
     * @param event     事件标识（如 approval.completed）
     * @param eventType 事件类型名（用于构造 payload）
     * @param payload   事件数据（map 形式，序列化为 JSON）
     */
    void publishEvent(String event, String eventType, Map<String, Object> payload);

    /**
     * 查询订阅了指定事件的启用 Webhook 列表
     *
     * @param event 事件标识
     * @return 启用且订阅了该事件的 Webhook
     */
    List<SysWebhook> listSubscribers(String event);
}
