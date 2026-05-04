package com.linan.office.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.office.entity.OfficeMeetingBooking;
import com.linan.office.service.IOfficeMeetingBookingService;
import jakarta.validation.Valid;
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

    private final IOfficeMeetingBookingService bookingService;

    public OfficeMeetingBookingController(IOfficeMeetingBookingService bookingService) {
        this.bookingService = bookingService;
    }

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
     * 新增会议预订（业务逻辑与事务下沉至 Service，保证并发安全）
     */
    @Operation(summary = "新增会议预订")
    @OperLog(title = "会议预订", businessType = 1)
    @RequirePermission("office:meeting:add")
    @PostMapping
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
        bookingService.saveBooking(booking);
        return Result.success(null);
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
