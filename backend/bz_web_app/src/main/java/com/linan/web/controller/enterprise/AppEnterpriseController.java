package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.PassAuth;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysNotice;
import com.linan.system.entity.SysNoticeRead;
import com.linan.system.entity.SysUser;
import com.linan.system.entity.TodoItem;
import com.linan.system.mapper.SysRoleMapper;
import com.linan.system.mapper.SysUserMapper;
import com.linan.system.service.ISysNoticeReadService;
import com.linan.system.service.ISysNoticeService;
import com.linan.system.service.ITodoItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业用户端 API 控制器
 * 提供工作台、通知公告、通讯录、待办事项等接口
 */
@Slf4j
@Tag(name = "员工门户", description = "用户前台 - 工作台/公告/通讯录/待办")
@RestController
@RequestMapping("/api/app/v1")
public class AppEnterpriseController {

    @Autowired
    private ISysNoticeService noticeService;

    @Autowired
    private ISysNoticeReadService noticeReadService;

    @Autowired
    private ITodoItemService todoItemService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    // ==================== 工作台概览 ====================

    /**
     * 工作台概览数据
     * GET /api/app/v1/dashboard
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        Map<String, Object> data = new HashMap<>();

        // 待办统计（容错：表不存在时返回0）
        try {
            long todoCount = todoItemService.count(new LambdaQueryWrapper<TodoItem>()
                    .eq(TodoItem::getUserId, userId)
                    .in(TodoItem::getStatus, 0, 1));
            long doneCount = todoItemService.count(new LambdaQueryWrapper<TodoItem>()
                    .eq(TodoItem::getUserId, userId)
                    .eq(TodoItem::getStatus, 2));
            long urgentCount = todoItemService.count(new LambdaQueryWrapper<TodoItem>()
                    .eq(TodoItem::getUserId, userId)
                    .eq(TodoItem::getPriority, 3)
                    .in(TodoItem::getStatus, 0, 1));
            data.put("todoCount", todoCount);
            data.put("doneCount", doneCount);
            data.put("urgentCount", urgentCount);
        } catch (Exception e) {
            data.put("todoCount", 0);
            data.put("doneCount", 0);
            data.put("urgentCount", 0);
        }

        // 最新通知（5条，包含通知和公告两种类型）
        try {
            Page<SysNotice> noticePage = noticeService.page(new Page<>(1, 5),
                    new LambdaQueryWrapper<SysNotice>()
                            .eq(SysNotice::getStatus, "1")
                            .orderByDesc(SysNotice::getCreateTime));
            data.put("latestNotices", noticePage.getRecords());
        } catch (Exception e) {
            data.put("latestNotices", List.of());
        }

        // 即将到期的待办（3条，容错）
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            List<TodoItem> upcomingTodos = todoItemService.list(new LambdaQueryWrapper<TodoItem>()
                    .eq(TodoItem::getUserId, userId)
                    .in(TodoItem::getStatus, 0, 1)
                    .ge(TodoItem::getDueDate, today)
                    .orderByAsc(TodoItem::getDueDate)
                    .last("LIMIT 3"));
            data.put("upcomingTodos", upcomingTodos);
        } catch (Exception e) {
            data.put("upcomingTodos", List.of());
        }

        return Result.success(data);
    }

    // ==================== 通知公告 ====================

    /**
     * 通知公告列表
     * GET /api/app/v1/notices?page=1&pageSize=10
     */
    @PassAuth
    @GetMapping("/notices")
    public Result<PageResult<SysNotice>> noticeList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            // 查询已发布的通知（包含通知和公告两种类型）
            LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<SysNotice>()
                    .eq(SysNotice::getStatus, "1")
                    .orderByDesc(SysNotice::getCreateTime);

            // 未登录的游客只能看到 scope=public 的公告
            Long userId = AuthInterceptor.getCurrentUserId();
            if (userId == null) {
                wrapper.eq(SysNotice::getScope, "public");
            }

            Page<SysNotice> p = new Page<>(page, pageSize);
            IPage<SysNotice> result = noticeService.page(p, wrapper);

            // 已读标记：登录用户可见自己已读的公告
            if (userId != null && result.getRecords() != null && !result.getRecords().isEmpty()) {
                List<Long> noticeIds = result.getRecords().stream()
                        .map(SysNotice::getId).toList();
                List<Long> readIds = noticeReadService.lambdaQuery()
                        .in(SysNoticeRead::getNoticeId, noticeIds)
                        .eq(SysNoticeRead::getUserId, userId)
                        .list().stream().map(SysNoticeRead::getNoticeId).toList();
                result.getRecords().forEach(n -> n.setIsRead(readIds.contains(n.getId())));
            }
            return Result.page(result);
        } catch (Exception e) {
            log.error("查询公告列表失败", e);
            return Result.error("查询公告失败");
        }
    }

    /**
     * 通知公告详情
     * GET /api/app/v1/notices/{id}
     */
    @PassAuth
    @GetMapping("/notices/{id}")
    public Result<SysNotice> noticeDetail(@PathVariable Long id) {
        SysNotice notice = noticeService.getById(id);
        if (notice == null || !"1".equals(notice.getStatus())) {
            return Result.error("公告不存在或已下架");
        }
        // 游客只能查看 public 范围的公告
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null && !"public".equals(notice.getScope())) {
            return Result.error("公告不存在或已下架");
        }
        // 已读回执：登录用户查看即记录已读（幂等：已存在则跳过）
        if (userId != null) {
            boolean alreadyRead = noticeReadService.lambdaQuery()
                    .eq(SysNoticeRead::getNoticeId, id)
                    .eq(SysNoticeRead::getUserId, userId)
                    .count() > 0;
            if (!alreadyRead) {
                SysNoticeRead read = new SysNoticeRead();
                read.setNoticeId(id);
                read.setUserId(userId);
                read.setReadTime(LocalDateTime.now());
                noticeReadService.save(read);
            }
            notice.setIsRead(true);
        }
        return Result.success(notice);
    }

    // ==================== 通讯录 ====================

    /**
     * 通讯录 - 员工列表
     * GET /api/app/v1/contacts?page=1&pageSize=20&keyword=xxx
     */
    @GetMapping("/contacts")
    public Result<PageResult<Map<String, Object>>> contactList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getStatus, "0"); // 只查正常状态用户（0=正常 1=停用）

            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w
                        .like(SysUser::getUserName, keyword)
                        .or().like(SysUser::getNickName, keyword)
                        .or().like(SysUser::getEmail, keyword)
                        .or().like(SysUser::getPhone, keyword));
            }

            wrapper.orderByAsc(SysUser::getUserName);

            Page<SysUser> p = new Page<>(page, pageSize);
            IPage<SysUser> result = userMapper.selectPage(p, wrapper);

            // 批量查询当页所有用户的角色，避免逐用户循环查询的 N+1 问题
            List<Long> userIds = result.getRecords().stream()
                    .map(SysUser::getId)
                    .collect(Collectors.toList());
            Map<Long, List<String>> roleNameMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                try {
                    for (Map<String, Object> row : roleMapper.selectRoleNamesByUserIds(userIds)) {
                        Long uid = ((Number) row.get("userId")).longValue();
                        roleNameMap.computeIfAbsent(uid, k -> new java.util.ArrayList<>())
                                .add(String.valueOf(row.get("roleName")));
                    }
                } catch (Exception e) {
                    log.warn("批量查询用户角色失败", e);
                }
            }

            // 脱敏：隐藏密码等敏感信息，附加角色信息
            List<Map<String, Object>> contacts = result.getRecords().stream().map(user -> {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", user.getId());
                map.put("userName", user.getUserName());
                map.put("nickName", user.getNickName());
                map.put("email", user.getEmail());
                map.put("phone", user.getPhone());
                map.put("sex", user.getSex());
                map.put("avatar", user.getAvatar());
                map.put("roleNames", roleNameMap.getOrDefault(user.getId(), List.of()));
                return map;
            }).collect(Collectors.toList());

            PageResult<Map<String, Object>> pageResult = new PageResult<>();
            pageResult.setTotal(result.getTotal());
            pageResult.setPageNum(result.getCurrent());
            pageResult.setPageSize(result.getSize());
            pageResult.setPages(result.getPages());
            pageResult.setList(contacts);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("查询通讯录失败", e);
            return Result.error("查询通讯录失败");
        }
    }

    // ==================== 待办事项 ====================

    /**
     * 我的待办列表
     * GET /api/app/v1/todos?page=1&pageSize=10&status=0
     */
    @GetMapping("/todos")
    public Result<PageResult<TodoItem>> todoList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            LambdaQueryWrapper<TodoItem> wrapper = new LambdaQueryWrapper<TodoItem>()
                    .eq(TodoItem::getUserId, userId);

            if (status != null) {
                wrapper.eq(TodoItem::getStatus, status);
            }

            wrapper.orderByDesc(TodoItem::getPriority)
                    .orderByAsc(TodoItem::getDueDate);

            Page<TodoItem> p = new Page<>(page, pageSize);
            IPage<TodoItem> result = todoItemService.page(p, wrapper);
            return Result.page(result);
        } catch (Exception e) {
            log.error("查询待办列表失败, userId={}", userId, e);
            return Result.error("查询待办失败");
        }
    }

    /**
     * 新增待办
     * POST /api/app/v1/todos
     */
    @PostMapping("/todos")
    public Result<Void> addTodo(@RequestBody TodoItem todo) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            todo.setUserId(userId);
            if (todo.getStatus() == null) {
                todo.setStatus(0);
            }
            if (todo.getPriority() == null) {
                todo.setPriority(1);
            }

            boolean ok = todoItemService.save(todo);
            return ok ? Result.success(null) : Result.error("新增失败");
        } catch (Exception e) {
            log.error("新增待办失败, userId={}", userId, e);
            return Result.error("新增待办失败");
        }
    }

    /**
     * 更新待办
     * PUT /api/app/v1/todos
     */
    @PutMapping("/todos")
    public Result<Void> updateTodo(@RequestBody TodoItem todo) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            // 确认是本人的待办
            TodoItem existing = todoItemService.getById(todo.getId());
            if (existing == null || !existing.getUserId().equals(userId)) {
                return Result.error("无权操作");
            }

            // 如果状态改为已完成，记录完成时间
            if (todo.getStatus() != null && todo.getStatus() == 2 && existing.getStatus() != 2) {
                todo.setCompletedTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }

            boolean ok = todoItemService.updateById(todo);
            return ok ? Result.success(null) : Result.error("更新失败");
        } catch (Exception e) {
            log.error("更新待办失败, userId={}", userId, e);
            return Result.error("更新待办失败");
        }
    }

    /**
     * 删除待办
     * DELETE /api/app/v1/todos/{id}
     */
    @DeleteMapping("/todos/{id}")
    public Result<Void> deleteTodo(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            TodoItem existing = todoItemService.getById(id);
            if (existing == null || !existing.getUserId().equals(userId)) {
                return Result.error("无权操作");
            }

            boolean ok = todoItemService.removeById(id);
            return ok ? Result.success(null) : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除待办失败, userId={}, todoId={}", userId, id, e);
            return Result.error("删除待办失败");
        }
    }

    // ==================== 用户资料 ====================

    /**
     * 更新个人资料
     * PUT /api/app/v1/user/profile
     */
    @PutMapping("/user/profile")
    public Result<Void> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            if (body.containsKey("nickName")) {
                user.setNickName(body.get("nickName"));
            }
            if (body.containsKey("email")) {
                user.setEmail(body.get("email"));
            }
            if (body.containsKey("phone")) {
                user.setPhone(body.get("phone"));
            }
            if (body.containsKey("sex")) {
                user.setSex(body.get("sex"));
            }

            userMapper.updateById(user);
            return Result.success(null);
        } catch (Exception e) {
            log.error("更新个人资料失败, userId={}", userId, e);
            return Result.error("更新资料失败");
        }
    }
}
