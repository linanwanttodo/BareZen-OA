package com.linan.web.controller.enterprise;

import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttClockRecord;
import com.linan.hr.service.IAttClockRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 考勤打卡 Controller（用户前台 /api/app/v1/attendance）
 *
 * <p>上下班打卡与月度回顾。打卡人身份一律从 JWT 解析，不信任前端传参；
 * 迟到/早退由服务端按 sys_config 规则计算，用户端只负责触发。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "考勤打卡", description = "用户前台 - 上下班打卡与月度回顾")
@RestController
@RequestMapping("/api/app/v1/attendance")
@RequiredArgsConstructor
public class AppClockController {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final IAttClockRecordService clockService;

    /**
     * 上班打卡
     * POST /api/app/v1/attendance/clock-in
     */
    @Operation(summary = "上班打卡")
    @PostMapping("/clock-in")
    public Result<AttClockRecord> clockIn() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        AttClockRecord record = clockService.clockIn(userId,
                AuthInterceptor.getCurrentUsername(), LocalDateTime.now());
        return Result.success(record);
    }

    /**
     * 下班打卡
     * POST /api/app/v1/attendance/clock-out
     */
    @Operation(summary = "下班打卡")
    @PostMapping("/clock-out")
    public Result<AttClockRecord> clockOut() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        AttClockRecord record = clockService.clockOut(userId,
                AuthInterceptor.getCurrentUsername(), LocalDateTime.now());
        return Result.success(record);
    }

    /**
     * 当月打卡记录（日历视图数据源）
     * GET /api/app/v1/attendance/clock/month?month=yyyy-MM
     */
    @Operation(summary = "当月打卡记录")
    @GetMapping("/clock/month")
    public Result<List<AttClockRecord>> month(@RequestParam(required = false) String month) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        YearMonth ym;
        try {
            ym = (month == null || month.isBlank())
                    ? YearMonth.now()
                    : YearMonth.parse(month.trim(), MONTH_FMT);
        } catch (DateTimeParseException e) {
            return Result.error("月份格式错误，应为 yyyy-MM");
        }
        return Result.success(clockService.listMonth(userId, ym));
    }

    /**
     * 今日打卡状态（页面初始化）
     * GET /api/app/v1/attendance/clock/today
     */
    @Operation(summary = "今日打卡状态")
    @GetMapping("/clock/today")
    public Result<AttClockRecord> today() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        return Result.success(clockService.today(userId, LocalDateTime.now()));
    }
}