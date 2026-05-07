package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linan.system.entity.SysWebhook;
import com.linan.system.mapper.SysWebhookMapper;
import com.linan.system.service.ISysWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Webhook 事件订阅服务实现
 *
 * <p>事件发生时向订阅方推送 JSON 回调，支持 HMAC-SHA256 签名（配置了 secret 时），
 * 回调头携带 {@code X-Webhook-Signature}，接收方可验签防伪造。
 * 使用 {@code @Async} 异步推送，不阻塞主业务；单次失败仅记录日志（重试策略可后续增强）。
 * JSON 序列化统一走 Jackson {@link ObjectMapper}，避免手写拼接导致特殊字符/转义错误。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Service
public class SysWebhookServiceImpl extends ServiceImpl<SysWebhookMapper, SysWebhook>
        implements ISysWebhookService {

    private final ObjectMapper objectMapper;

    public SysWebhookServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SysWebhook> listSubscribers(String event) {
        return list(new LambdaQueryWrapper<SysWebhook>()
                        .eq(SysWebhook::getStatus, "0")          // 仅启用
                        .orderByAsc(SysWebhook::getCreateTime))
                .stream()
                // events 逗号分隔，取与目标事件精确相等的项（避免 like 子串误匹配）
                .filter(w -> subscribedTo(w.getEvents(), event))
                .collect(Collectors.toList());
    }

    /**
     * 判断 subscriberEvents（逗号分隔）是否精确订阅了 event。
     * 首尾 trim 后整段精确比较，规避 "notice" 命中 "notice.published" 这类子串误报。
     */
    private boolean subscribedTo(String subscriberEvents, String event) {
        if (subscriberEvents == null || event == null) {
            return false;
        }
        for (String sub : subscriberEvents.split(",")) {
            if (event.equals(sub.trim())) {
                return true;
            }
        }
        return false;
    }

    @Async
    @Override
    public void publishEvent(String event, String eventType, Map<String, Object> payload) {
        List<SysWebhook> subscribers = listSubscribers(event);
        if (subscribers.isEmpty()) {
            return;
        }

        // 构造统一 payload：{ event, eventType, timestamp, data }
        Map<String, Object> body = new HashMap<>();
        body.put("event", event);
        body.put("eventType", eventType);
        body.put("timestamp", Instant.now().getEpochSecond());
        body.put("data", payload != null ? payload : Map.of());

        for (SysWebhook webhook : subscribers) {
            try {
                String json = toJson(body);
                String signature = sign(webhook, json);
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(5))
                        .build();
                java.net.http.HttpRequest.Builder reqBuilder = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(webhook.getUrl()))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .header("User-Agent", "BareZen-OpenAPI/1.0")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
                if (signature != null) {
                    reqBuilder.header("X-Webhook-Signature", signature);
                }
                java.net.http.HttpResponse<String> resp = client.send(reqBuilder.build(),
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 400) {
                    log.warn("Webhook 推送失败: url={}, status={}, body={}",
                            webhook.getUrl(), resp.statusCode(), truncate(resp.body(), 200));
                }
            } catch (Exception e) {
                log.warn("Webhook 推送异常: url={}, err={}", webhook.getUrl(), e.getMessage());
            }
        }
    }

    /**
     * 委托 Jackson 序列化 JSON（正确处理引号/反斜杠/换行/控制字符等特殊字符）。
     */
    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Webhook JSON 序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * HMAC-SHA256 签名（配置了 secret 时启用；未配置返回 null 不签名）
     */
    private String sign(SysWebhook webhook, String payload) {
        if (webhook.getSecret() == null || webhook.getSecret().isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhook.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return "sha256=" + sb;
        } catch (Exception e) {
            log.warn("Webhook 签名失败: {}", e.getMessage());
            return null;
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
