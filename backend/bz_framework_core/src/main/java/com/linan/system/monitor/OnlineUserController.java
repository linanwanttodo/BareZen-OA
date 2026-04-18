package com.linan.system.monitor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/v1/monitor/online")
public class OnlineUserController {

    @Autowired
    private OnlineUserService onlineUserService;

    @GetMapping("/list")
    public Result<IPage<OnlineUser>> list(
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

        return Result.success(page);
    }

    @GetMapping("/count")
    public Result<Long> count() {
        return Result.success(onlineUserService.countOnlineUsers());
    }

    @RequirePermission("monitor:online:delete")
    @DeleteMapping("/{token}")
    public Result<Void> forceLogout(@PathVariable String token) {
        onlineUserService.forceLogout(token);
        return Result.success(null);
    }
}
