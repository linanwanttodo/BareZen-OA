package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageParamGuard;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeMeetingBooking;
import com.linan.office.entity.OfficeMeetingRoom;
import com.linan.office.service.IOfficeMeetingBookingService;
import com.linan.office.service.IOfficeMeetingRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会议预订 Controller（用户前台 /api/app/v1/meetings）
 *
 * <p>员工自助预订会议室、查询我的预订与详情、取消本人预订。
 * 与管理后台的会议接口（/api/office/v1/bookings、/api/office/v1/rooms）分离：
 * 这里仅需登录即可使用，预订人自动填充为当前登录用户。</p>
 *
 * <p>会议预订不接入审批流（与管理后台口径一致）：创建即生效，取消即释放房间占用。
 * 创建/冲突校验复用 {@link IOfficeMeetingBookingService#saveBooking} 的事务内行锁机制，
 * 保证并发防冲突口径与管理端完全一致。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "会议预订", description = "用户前台 - 会议预订")
@RestController
@RequestMapping("/api/app/v1/meetings")
@RequiredArgsConstructor
public class AppMeetingController {

    private final IOfficeMeetingBookingService bookingService;

    private final IOfficeMeetingRoomService roomService;

    /**
     * 可用会议室列表（启用状态，供预订下拉选择）
     * GET /api/app/v1/meetings/rooms
     */
    @Operation(summary = "可用会议室列表")
    @GetMapping("/rooms")
    public Result<List<OfficeMeetingRoom>> rooms() {
        return Result.success(listEnabledRooms());
    }

    /**
     * 指定时段可用的会议室列表（排除该时段已被预订的会议室）
     * GET /api/app/v1/meetings/rooms/available?start=&end=
     *
     * <p>时间区间重叠判定与 {@link IOfficeMeetingBookingService#saveBooking} 的冲突校验同口径：
     * {@code start < booking.endTime && end > booking.startTime}（字符串字典序即时间序）。</p>
     */
    @Operation(summary = "指定时段可用会议室列表")
    @GetMapping("/rooms/available")
    public Result<List<OfficeMeetingRoom>> availableRooms(@RequestParam String start, @RequestParam String end) {
        if (start == null || start.isEmpty() || end == null || end.isEmpty()) {
            return Result.error("请选择查询时段");
        }
        if (start.compareTo(end) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }
        List<OfficeMeetingRoom> enabledRooms = listEnabledRooms();
        if (enabledRooms.isEmpty()) {
            return Result.success(enabledRooms);
        }
        // 该时段已被占用的会议室（区间重叠即占用）
        List<Long> roomIds = enabledRooms.stream().map(OfficeMeetingRoom::getId).toList();
        Set<Long> busyRoomIds = bookingService.list(new LambdaQueryWrapper<OfficeMeetingBooking>()
                        .select(OfficeMeetingBooking::getRoomId)
                        .in(OfficeMeetingBooking::getRoomId, roomIds)
                        .lt(OfficeMeetingBooking::getStartTime, end)
                        .gt(OfficeMeetingBooking::getEndTime, start))
                .stream().map(OfficeMeetingBooking::getRoomId).collect(Collectors.toSet());
        List<OfficeMeetingRoom> freeRooms = enabledRooms.stream()
                .filter(room -> !busyRoomIds.contains(room.getId()))
                .toList();
        return Result.success(freeRooms);
    }

    /**
     * 我的会议预订列表
     * GET /api/app/v1/meetings
     */
    @Operation(summary = "我的会议预订列表")
    @GetMapping
    public Result<PageResult<OfficeMeetingBooking>> myBookings(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 分页参数钳制
        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<OfficeMeetingBooking> wrapper = new LambdaQueryWrapper<OfficeMeetingBooking>()
                .eq(OfficeMeetingBooking::getBookerId, userId)
                .orderByDesc(OfficeMeetingBooking::getStartTime);
        IPage<OfficeMeetingBooking> result = bookingService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result);
    }

    /**
     * 会议预订详情
     * GET /api/app/v1/meetings/{id}
     */
    @Operation(summary = "会议预订详情")
    @GetMapping("/{id}")
    public Result<OfficeMeetingBooking> detail(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        OfficeMeetingBooking booking = bookingService.getById(id);
        if (booking == null) {
            return Result.error("记录不存在");
        }
        // 归属校验：仅允许查看本人提交的预订
        if (!userId.equals(booking.getBookerId())) {
            return Result.error("无权查看该预订");
        }
        return Result.success(booking);
    }

    /**
     * 提交会议预订
     * POST /api/app/v1/meetings
     *
     * <p>复用管理端的 {@link IOfficeMeetingBookingService#saveBooking}：
     * 事务内行锁锁定会议室行 + 时间冲突校验，冲突抛 {@link com.linan.core.common.exception.BusinessException}
     * 向上传播（全局异常处理器统一返回、事务回滚），防冲突口径与管理端一致。
     * 不接入审批流，创建即生效。</p>
     */
    @Operation(summary = "提交会议预订")
    @OperLog(title = "会议预订", businessType = 1)
    @Transactional(rollbackFor = Exception.class)
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody OfficeMeetingBooking booking) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (booking.getRoomId() == null) {
            return Result.error("请选择会议室");
        }
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            return Result.error("请选择会议时间");
        }
        if (booking.getStartTime().compareTo(booking.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }
        // 强制清空客户端可能预置的敏感字段（防主键冲突、冗余字段污染）
        booking.setId(null);
        booking.setRoomName(null);
        // 身份字段服务端强制覆盖，不信任请求体（防身份伪造）
        booking.setBookerId(userId);
        String username = AuthInterceptor.getCurrentUsername();
        booking.setBookerName(username != null ? username : "用户" + userId);
        bookingService.saveBooking(booking);
        return Result.success(null);
    }

    /**
     * 取消我的会议预订
     * PUT /api/app/v1/meetings/{id}/cancel
     *
     * <p>仅本人可取消；会议预订无审批状态字段，取消即删除预订记录，房间占用随之释放。
     * 带归属条件的删除与并发编辑竞态时保证不误删他人数据。</p>
     */
    @Operation(summary = "取消会议预订")
    @OperLog(title = "会议预订", businessType = 3)
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        OfficeMeetingBooking booking = bookingService.getById(id);
        if (booking == null) {
            return Result.error("记录不存在");
        }
        // 归属校验：仅允许取消本人提交的预订
        if (!userId.equals(booking.getBookerId())) {
            return Result.error("无权取消该预订");
        }
        // 原子条件删除：id + bookerId 双条件，避免并发窗口内记录归属变化被误删
        boolean ok = bookingService.remove(new LambdaQueryWrapper<OfficeMeetingBooking>()
                .eq(OfficeMeetingBooking::getId, id)
                .eq(OfficeMeetingBooking::getBookerId, userId));
        return ok ? Result.success(null) : Result.error("取消失败，请刷新后重试");
    }

    /** 启用状态会议室列表（预订下拉与时段可用查询共用） */
    private List<OfficeMeetingRoom> listEnabledRooms() {
        return roomService.list(new LambdaQueryWrapper<OfficeMeetingRoom>()
                .eq(OfficeMeetingRoom::getStatus, "0")
                .orderByAsc(OfficeMeetingRoom::getRoomName));
    }
}
