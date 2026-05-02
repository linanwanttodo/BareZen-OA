package com.linan.office.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.office.entity.OfficeMeetingRoom;
import com.linan.office.service.IOfficeMeetingRoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 会议室管理 Controller
 *
 * @author BareZen Team
 */
@Tag(name = "会议室管理", description = "行政办公 - 会议室管理")
@RestController
@RequestMapping("/api/office/v1/rooms")
public class OfficeMeetingRoomController {

    @Autowired
    private IOfficeMeetingRoomService roomService;

    /**
     * 分页查询会议室
     */
    @RequirePermission("office:meeting:list")
    @Operation(summary = "分页查询会议室")
    @GetMapping("/page")
    public Result<PageResult<OfficeMeetingRoom>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String status) {

        Page<OfficeMeetingRoom> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OfficeMeetingRoom> wrapper = new LambdaQueryWrapper<>();

        if (roomName != null && !roomName.isEmpty()) {
            wrapper.like(OfficeMeetingRoom::getRoomName, roomName);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OfficeMeetingRoom::getStatus, status);
        }
        wrapper.orderByAsc(OfficeMeetingRoom::getRoomName);
        IPage<OfficeMeetingRoom> result = roomService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 可用会议室列表（供预订下拉选择）
     */
    @RequirePermission("office:meeting:list")
    @Operation(summary = "可用会议室列表")
    @GetMapping("/available")
    public Result<List<OfficeMeetingRoom>> available() {
        LambdaQueryWrapper<OfficeMeetingRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OfficeMeetingRoom::getStatus, "0")
                .orderByAsc(OfficeMeetingRoom::getRoomName);
        return Result.success(roomService.list(wrapper));
    }

    /**
     * 新增会议室
     */
    @Operation(summary = "新增会议室")
    @OperLog(title = "会议室管理", businessType = 1)
    @RequirePermission("office:meeting:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody OfficeMeetingRoom room) {
        if (room.getStatus() == null || room.getStatus().isEmpty()) {
            room.setStatus("0");
        }
        boolean ok = roomService.save(room);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新会议室
     */
    @Operation(summary = "修改会议室")
    @OperLog(title = "会议室管理", businessType = 2)
    @RequirePermission("office:meeting:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody OfficeMeetingRoom room) {
        boolean ok = roomService.updateById(room);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除会议室
     */
    @Operation(summary = "删除会议室")
    @OperLog(title = "会议室管理", businessType = 3)
    @RequirePermission("office:meeting:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = roomService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}
