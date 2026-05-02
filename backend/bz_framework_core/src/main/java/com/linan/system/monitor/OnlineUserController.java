package com.linan.system.monitor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "在线用户", description = "在线用户监控与强制下线")
@RestController
@RequestMapping("/api/system/v1/monitor/online")
public class OnlineUserController {

    @Autowired
    private OnlineUserService onlineUserService;

    @RequirePermission("monitor:online:list")
    @Operation(summary = "分页查询在线用户")
    @GetMapping("/page")
    public Result<PageResult<OnlineUser>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        List<OnlineUser> allUsers = onlineUserService.listOnlineUsers();

        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, allUsers.size());
        List<OnlineUser> pageList = start < allUsers.size()
                ? allUsers.subList(start, end)
                : List.of();

        Page<OnlineUser> page = new Page<>(pageNum, pageSize);
        page.setTotal(allUsers.size());
        page.setRecords(pageList);

        return Result.page(page);
    }

    @RequirePermission("monitor:online:list")
    @Operation(summary = "统计在线用户数量")
    @GetMapping("/count")
    public Result<Long> count() {
        return Result.success(onlineUserService.countOnlineUsers());
    }

    @RequirePermission("monitor:online:delete")
    @Operation(summary = "强制用户下线")
    @DeleteMapping("/forceLogout")
    public Result<Void> forceLogout(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isEmpty()) {
            return Result.error("token 不能为空");
        }
        // 支持传入完整 token 或 tokenDigest
        onlineUserService.forceLogout(token);
        return Result.success(null);
    }
}
