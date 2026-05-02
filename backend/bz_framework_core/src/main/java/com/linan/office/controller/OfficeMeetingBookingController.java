package com.linan.office.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeMeetingBooking;
import com.linan.office.entity.OfficeMeetingRoom;
import com.linan.office.mapper.OfficeMeetingRoomMapper;
import com.linan.office.service.IOfficeMeetingBookingService;
import com.linan.office.service.IOfficeMeetingRoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 会议预订 Controller
 *
 * <p>预订时自动填充预订人，并校验所选会议室是否存在、时间是否冲突。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "会议预订", description = "行政办公 - 会议预订")
@RestController
@RequestMapping("/api/office/v1/bookings")
public class OfficeMeetingBookingController {

    @Autowired
    private IOfficeMeetingBookingService bookingService;

    @Autowired
    private IOfficeMeetingRoomService roomService;

    @Autowired
    private OfficeMeetingRoomMapper roomMapper;

    /**
     * 分页查询会议预订
     */
    @RequirePermission("office:meeting:list")
    @Operation(summary = "分页查询会议预订")
    @GetMapping("/page")
    public Result<PageResult<OfficeMeetingBooking>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long roomId) {

        Page<OfficeMeetingBooking> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OfficeMeetingBooking> wrapper = new LambdaQueryWrapper<>();

        if (title != null && !title.isEmpty()) {
            wrapper.like(OfficeMeetingBooking::getTitle, title);
        }
        if (roomId != null) {
            wrapper.eq(OfficeMeetingBooking::getRoomId, roomId);
        }
        wrapper.orderByDesc(OfficeMeetingBooking::getStartTime);
        IPage<OfficeMeetingBooking> result = bookingService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 新增会议预订（事务内行锁 + 时间冲突校验，保证并发安全）
     */
    @Operation(summary = "新增会议预订")
    @OperLog(title = "会议预订", businessType = 1)
    @RequirePermission("office:meeting:add")
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> save(@Valid @RequestBody OfficeMeetingBooking booking) {
        if (booking.getRoomId() == null) {
            return Result.error("请选择会议室");
        }
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            return Result.error("请选择会议时间");
        }
        if (booking.getStartTime().compareTo(booking.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }

        // 悲观锁：锁定会议室行，使同一会议室的并发预订串行化
        // （仅当锁查询结果为空时回退到普通查询，兼容无 FOR UPDATE 权限的只读场景）
        OfficeMeetingRoom room = roomMapper.selectByIdForUpdate(booking.getRoomId());
        if (room == null) {
            return Result.error("会议室不存在");
        }
        booking.setRoomName(room.getRoomName());

        // 时间冲突校验：同一会议室时间区间重叠则拒绝
        long conflict = bookingService.lambdaQuery()
                .eq(OfficeMeetingBooking::getRoomId, booking.getRoomId())
                .and(w -> w
                        .lt(OfficeMeetingBooking::getStartTime, booking.getEndTime())
                        .gt(OfficeMeetingBooking::getEndTime, booking.getStartTime()))
                .count();
        if (conflict > 0) {
            return Result.error("该会议室在所选时间段已被预订");
        }

        Long userId = AuthInterceptor.getCurrentUserId();
        booking.setBookerId(userId);
        if (booking.getBookerName() == null || booking.getBookerName().isEmpty()) {
            booking.setBookerName(userId != null ? String.valueOf(userId) : null);
        }
        boolean ok = bookingService.save(booking);
        return ok ? Result.success(null) : Result.error("预订失败");
    }

    /**
     * 删除会议预订
     */
    @Operation(summary = "删除会议预订")
    @OperLog(title = "会议预订", businessType = 3)
    @RequirePermission("office:meeting:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = bookingService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}
