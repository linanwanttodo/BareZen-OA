package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.hr.domain.AttClockMonthlyStat;
import com.linan.hr.service.IAttClockRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 考勤统计 Controller（管理后台 /api/hr/v1/clock）
 *
 * <p>月度考勤统计：按员工分组聚合出勤/迟到/早退/缺卡，聚合在 SQL 层完成，
 * 不把明细拉进内存统计。部门/姓名筛选可选。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "考勤统计", description = "考勤管理 - 月度考勤统计")
@RestController
@RequestMapping("/api/hr/v1/clock")
@RequiredArgsConstructor
public class AttClockController {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final IAttClockRecordService clockService;

    /**
     * 月度考勤统计（按员工分组，分页）
     * GET /api/hr/v1/clock/page?month=yyyy-MM&pageNum=1&pageSize=10&userName=&deptId=
     */
    @RequirePermission("hr:attendance:list")
    @Operation(summary = "月度考勤统计（按员工分组）")
    @GetMapping("/page")
    public Result<PageResult<AttClockMonthlyStat>> page(
            @RequestParam String month,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Long deptId) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month.trim(), MONTH_FMT);
        } catch (DateTimeParseException e) {
            return Result.error("月份格式错误，应为 yyyy-MM");
        }
        IPage<AttClockMonthlyStat> result = clockService.pageMonthlyStat(
                pageNum, pageSize, ym, userName, deptId);
        return Result.page(result);
    }
}