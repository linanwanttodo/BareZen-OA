package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysMessage;
import com.linan.system.service.ISysMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内消息中心 Controller（用户前台 /api/app/v1/messages）
 *
 * <p>WebSocket 离线兜底：审批完成等事件在 WS 推送失败（用户离线）时写入
 * {@code sys_message}，员工登录后在此查看、标记已读。当前用户身份取自 JWT。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "站内消息", description = "用户前台 - 消息中心（离线通知兜底）")
@RestController
@RequestMapping("/api/app/v1/messages")
@RequiredArgsConstructor
public class AppMessageController {

    private final ISysMessageService messageService;

    /**
     * 分页查询我的站内信
     * GET /api/app/v1/messages?page=1&pageSize=10&isRead=0
     */
    @Operation(summary = "分页查询我的站内信", description = "按创建时间倒序；isRead 可空（不传=全部，0=未读，1=已读）")
    @GetMapping
    public Result<PageResult<SysMessage>> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "已读过滤：空=全部 0=未读 1=已读") @RequestParam(required = false) Integer isRead) {

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        IPage<SysMessage> result = messageService.pageMine(new Page<>(page, pageSize), userId, isRead);
        return Result.page(result);
    }

    /**
     * 未读数量
     * GET /api/app/v1/messages/unread-count
     */
    @Operation(summary = "未读消息数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        return Result.success(messageService.unreadCount(userId));
    }

    /**
     * 标记单条已读（仅限本人消息）
     * PUT /api/app/v1/messages/{id}/read
     */
    @Operation(summary = "标记单条已读", description = "仅本人消息有效，重复标记幂等")
    @PutMapping("/{id}/read")
    public Result<Boolean> markRead(
            @Parameter(description = "消息ID") @PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (!messageService.markRead(id, userId)) {
            return Result.error("消息不存在或无权操作");
        }
        return Result.success(true);
    }

    /**
     * 全部标记已读
     * PUT /api/app/v1/messages/read-all
     */
    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Integer> markAllRead() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        return Result.success(messageService.markAllRead(userId));
    }

    /**
     * 查询单条消息详情（校验属主 + 自动标记已读）
     * GET /api/app/v1/messages/{id}
     */
    @Operation(summary = "查询单条消息详情", description = "校验消息属于当前用户，并自动标记为已读")
    @GetMapping("/{id}")
    public Result<SysMessage> getDetail(
            @Parameter(description = "消息ID") @PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        SysMessage message = messageService.getById(id);
        if (message == null) {
            return Result.error("消息不存在");
        }
        
        // 校验属主
        if (!userId.equals(message.getUserId())) {
            return Result.error("无权查看该消息");
        }
        
        // 自动标记已读（幂等）
        if (message.getIsRead() != null && message.getIsRead() == 0) {
            messageService.markRead(id, userId);
            message.setIsRead(1);
        }
        
        return Result.success(message);
    }
}