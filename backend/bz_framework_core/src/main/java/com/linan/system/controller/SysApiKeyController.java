package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysApiKey;
import com.linan.system.service.ISysApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 开放平台 API Key 管理 Controller（管理后台）
 *
 * <p>对应 sys_api_key 表的管理入口：分页列表（明文脱敏展示）、创建
 * （生成随机 Key，明文只在创建响应返回一次）、启停、删除。
 * 存储策略与校验侧一致：库中仅存 SHA-256 哈希，见 {@link ISysApiKeyService#createApiKey}。</p>
 *
 * <p>列表脱敏规则：明文不落库，无法还原，故以「bz_**** + 哈希末 4 位」作为
 * 稳定的脱敏标识（哈希不可逆，末 4 位仅用于区分，无泄露风险）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "开放平台-API Key 管理", description = "开放平台 API Key 的分页/创建/启停/删除管理")
@RestController
@RequestMapping("/api/system/v1/api-keys")
@RequiredArgsConstructor
public class SysApiKeyController {

    private static final String KEY_PREFIX = "bz_";

    private final ISysApiKeyService apiKeyService;

    /**
     * 分页查询 API Key（明文脱敏展示）
     */
    @RequirePermission("system:apiKey:list")
    @Operation(summary = "分页查询 API Key（明文脱敏）")
    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "密钥名称模糊查询（可选）") @RequestParam(required = false) String keyName) {

        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            return Result.error("分页参数不合法");
        }

        LambdaQueryWrapper<SysApiKey> wrapper = new LambdaQueryWrapper<>();
        if (keyName != null && !keyName.isEmpty()) {
            wrapper.like(SysApiKey::getKeyName, keyName);
        }
        wrapper.orderByDesc(SysApiKey::getCreateTime);

        IPage<SysApiKey> page = apiKeyService.page(new Page<>(pageNum, pageSize), wrapper);
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setPages(page.getPages());
        result.setList(page.getRecords().stream().map(this::toMaskedView).toList());
        return Result.success(result);
    }

    /**
     * 创建 API Key
     *
     * <p>body: {@code {"keyName":"xxx", "scopes":"open:approval:read", "expireTime":"2026-12-31 23:59:59"}}
     * scopes/expireTime 可选；明文 Key 仅在本次响应 data.apiKey 中出现一次。</p>
     */
    @RequirePermission("system:apiKey:add")
    @OperLog(title = "开放平台 API Key", businessType = 1)
    @Operation(summary = "创建 API Key（明文仅本次返回一次）")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        String keyName = body.get("keyName");
        if (keyName == null || keyName.isBlank()) {
            return Result.error("密钥名称不能为空");
        }
        if (keyName.length() > 50) {
            return Result.error("密钥名称不能超过 50 个字符");
        }
        String scopes = body.get("scopes");
        LocalDateTime expireTime = parseExpireTime(body.get("expireTime"));

        ISysApiKeyService.SysApiKeyCreated created = apiKeyService.createApiKey(
                keyName.trim(), scopes, expireTime);

        Map<String, Object> data = new HashMap<>();
        data.put("id", created.id());
        data.put("apiKey", created.apiKey());   // 明文：仅此一次返回
        data.put("keyName", created.keyName());
        data.put("scopes", created.scopes());
        data.put("status", created.status());
        data.put("expireTime", created.expireTime());
        return Result.success(data);
    }

    /**
     * 启用/禁用 API Key（status: 0=启用 1=禁用）
     */
    @RequirePermission("system:apiKey:edit")
    @OperLog(title = "开放平台 API Key", businessType = 2)
    @Operation(summary = "启用/禁用 API Key")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(
            @Parameter(description = "密钥ID") @PathVariable Long id,
            @Parameter(description = "状态：0=启用 1=禁用") @PathVariable String status) {
        if (!"0".equals(status) && !"1".equals(status)) {
            return Result.error("状态值不合法（仅 0=启用 1=禁用）");
        }
        SysApiKey key = apiKeyService.getById(id);
        if (key == null) {
            return Result.error("API Key 不存在");
        }
        SysApiKey update = new SysApiKey();
        update.setId(id);
        update.setStatus(status);
        boolean ok = apiKeyService.updateById(update);
        return ok ? Result.success(null) : Result.error("状态切换失败");
    }

    /**
     * 删除 API Key（逻辑删除）
     */
    @RequirePermission("system:apiKey:delete")
    @OperLog(title = "开放平台 API Key", businessType = 3)
    @Operation(summary = "删除 API Key")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "密钥ID") @PathVariable Long id) {
        SysApiKey key = apiKeyService.getById(id);
        if (key == null) {
            return Result.error("API Key 不存在");
        }
        boolean ok = apiKeyService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 构造脱敏视图：仅暴露 maskedKey，绝不返回哈希与明文
     */
    private Map<String, Object> toMaskedView(SysApiKey key) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", key.getId());
        m.put("keyName", key.getKeyName());
        m.put("maskedKey", mask(key.getApiKeyHash()));
        m.put("scopes", key.getScopes());
        m.put("status", key.getStatus());
        m.put("expireTime", key.getExpireTime());
        m.put("createTime", key.getCreateTime());
        return m;
    }

    /**
     * 脱敏展示：bz_**** + 哈希末 4 位（哈希不可逆，仅作稳定区分标识）
     */
    private String mask(String hash) {
        if (hash == null || hash.isEmpty()) {
            return KEY_PREFIX + "****";
        }
        int tail = Math.min(4, hash.length());
        return KEY_PREFIX + "****" + hash.substring(hash.length() - tail);
    }

    private LocalDateTime parseExpireTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim().replace(' ', 'T'));
        } catch (Exception e) {
            throw new IllegalArgumentException("expireTime 格式不合法（应为 yyyy-MM-dd HH:mm:ss）");
        }
    }
}