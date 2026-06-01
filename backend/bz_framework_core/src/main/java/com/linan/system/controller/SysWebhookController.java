package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysWebhook;
import com.linan.system.service.ISysWebhookService;
import com.linan.system.service.impl.SysWebhookServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 开放平台 Webhook 管理 Controller（管理后台）
 *
 * <p>对应 sys_webhook 表的管理入口：分页列表（secret 脱敏展示）、
 * 新增订阅（URL + 事件类型）、启停、删除。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "开放平台-Webhook 管理", description = "Webhook 事件订阅的分页/新增/启停/删除管理")
@RestController
@RequestMapping("/api/system/v1/webhooks")
@RequiredArgsConstructor
public class SysWebhookController {

    private final ISysWebhookService webhookService;

    /**
     * 分页查询 Webhook 订阅（secret 脱敏展示）
     */
    @RequirePermission("system:webhook:list")
    @Operation(summary = "分页查询 Webhook 订阅")
    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "订阅名称模糊查询（可选）") @RequestParam(required = false) String name) {

        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            return Result.error("分页参数不合法");
        }

        LambdaQueryWrapper<SysWebhook> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(SysWebhook::getName, name);
        }
        wrapper.orderByDesc(SysWebhook::getCreateTime);

        IPage<SysWebhook> page = webhookService.page(new Page<>(pageNum, pageSize), wrapper);
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setPages(page.getPages());
        result.setList(page.getRecords().stream().map(this::toView).toList());
        return Result.success(result);
    }

    /**
     * 新增 Webhook 订阅
     *
     * <p>body: {@code {"name":"xxx", "url":"https://...", "secret":"可选", "events":"approval.completed"}}
     * events 为逗号分隔的事件标识。</p>
     */
    @RequirePermission("system:webhook:add")
    @OperLog(title = "开放平台 Webhook", businessType = 1)
    @Operation(summary = "新增 Webhook 订阅")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String url = body.get("url");
        String events = body.get("events");
        if (name == null || name.isBlank()) {
            return Result.error("订阅名称不能为空");
        }
        if (name.length() > 50) {
            return Result.error("订阅名称不能超过 50 个字符");
        }
        if (url == null || url.isBlank()) {
            return Result.error("回调地址不能为空");
        }
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            return Result.error("回调地址必须以 http:// 或 https:// 开头");
        }
        // SSRF 防护：禁止回调内网地址
        if (SysWebhookServiceImpl.isInternalUrl(url.trim())) {
            return Result.error("回调地址不允许指向内网地址（如 localhost / 192.168.x.x 等）");
        }
        if (events == null || events.isBlank()) {
            return Result.error("订阅事件不能为空");
        }

        SysWebhook webhook = new SysWebhook();
        webhook.setName(name.trim());
        webhook.setUrl(url.trim());
        webhook.setSecret(body.get("secret"));
        webhook.setEvents(events.trim());
        webhook.setStatus("0");
        boolean ok = webhookService.save(webhook);
        if (!ok) {
            return Result.error("新增失败");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", webhook.getId());
        data.put("name", webhook.getName());
        data.put("url", webhook.getUrl());
        data.put("secret", webhook.getSecret() != null && !webhook.getSecret().isBlank() ? "******" : null);
        data.put("events", webhook.getEvents());
        data.put("status", webhook.getStatus());
        return Result.success(data);
    }

    /**
     * 启用/禁用 Webhook（status: 0=启用 1=禁用）
     */
    @RequirePermission("system:webhook:edit")
    @OperLog(title = "开放平台 Webhook", businessType = 2)
    @Operation(summary = "启用/禁用 Webhook")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(
            @Parameter(description = "订阅ID") @PathVariable Long id,
            @Parameter(description = "状态：0=启用 1=禁用") @PathVariable String status) {
        if (!"0".equals(status) && !"1".equals(status)) {
            return Result.error("状态值不合法（仅 0=启用 1=禁用）");
        }
        SysWebhook webhook = webhookService.getById(id);
        if (webhook == null) {
            return Result.error("订阅不存在");
        }
        SysWebhook update = new SysWebhook();
        update.setId(id);
        update.setStatus(status);
        boolean ok = webhookService.updateById(update);
        return ok ? Result.success(null) : Result.error("状态切换失败");
    }

    /**
     * 编辑 Webhook（更新名称、URL、secret、事件）
     *
     * <p>body: {@code {"name":"xxx", "url":"https://...", "secret":"可选", "events":"approval.completed"}}
     * secret 可选传空字符串则清空。</p>
     */
    @RequirePermission("system:webhook:edit")
    @OperLog(title = "开放平台 Webhook", businessType = 2)
    @Operation(summary = "编辑 Webhook")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "订阅ID") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        SysWebhook webhook = webhookService.getById(id);
        if (webhook == null) {
            return Result.error("订阅不存在");
        }
        String name = body.get("name");
        String url = body.get("url");
        String events = body.get("events");
        if (name == null || name.isBlank()) {
            return Result.error("订阅名称不能为空");
        }
        if (name.length() > 50) {
            return Result.error("订阅名称不能超过 50 个字符");
        }
        if (url == null || url.isBlank()) {
            return Result.error("回调地址不能为空");
        }
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            return Result.error("回调地址必须以 http:// 或 https:// 开头");
        }
        // SSRF 防护：禁止回调内网地址
        if (SysWebhookServiceImpl.isInternalUrl(url.trim())) {
            return Result.error("回调地址不允许指向内网地址（如 localhost / 192.168.x.x 等）");
        }
        if (events == null || events.isBlank()) {
            return Result.error("订阅事件不能为空");
        }
        SysWebhook update = new SysWebhook();
        update.setId(id);
        update.setName(name.trim());
        update.setUrl(url.trim());
        update.setSecret(body.get("secret"));
        update.setEvents(events.trim());
        boolean ok = webhookService.updateById(update);
        return ok ? Result.success(null) : Result.error("编辑失败");
    }

    /**
     * 删除 Webhook 订阅（逻辑删除）
     */
    @RequirePermission("system:webhook:delete")
    @OperLog(title = "开放平台 Webhook", businessType = 3)
    @Operation(summary = "删除 Webhook 订阅")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "订阅ID") @PathVariable Long id) {
        SysWebhook webhook = webhookService.getById(id);
        if (webhook == null) {
            return Result.error("订阅不存在");
        }
        boolean ok = webhookService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 列表视图：secret 一律脱敏为掩码，不返回明文
     */
    private Map<String, Object> toView(SysWebhook webhook) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", webhook.getId());
        m.put("name", webhook.getName());
        m.put("url", webhook.getUrl());
        m.put("secret", webhook.getSecret() != null && !webhook.getSecret().isBlank() ? "******" : null);
        m.put("events", webhook.getEvents());
        m.put("status", webhook.getStatus());
        m.put("createTime", webhook.getCreateTime());
        return m;
    }
}