package com.linan.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.hr.domain.AttClockMonthlyStat;
import com.linan.hr.entity.AttClockRecord;
import com.linan.hr.mapper.AttClockRecordMapper;
import com.linan.hr.service.IAttClockRecordService;
import com.linan.system.service.ISysConfigService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 考勤打卡记录服务实现
 *
 * <p>打卡规则（上班/下班时间、迟到宽容分钟）全部读取 sys_config，
 * 配置缺失或异常时回退默认值（09:00 / 18:00 / 0），不阻断打卡。
 * 同一用户同日一条记录：上班打卡新建、下班打卡条件更新
 * （clock_out_time IS NULL 兜底并发重复），重复打卡由业务校验拒绝。</p>
 *
 * @author BareZen Team
 */
@Service
public class AttClockRecordServiceImpl extends ServiceImpl<AttClockRecordMapper, AttClockRecord>
        implements IAttClockRecordService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    /** sys_config 键：上班时间（HH:mm） */
    private static final String KEY_WORK_START = "hr.attendance.work_start_time";
    /** sys_config 键：下班时间（HH:mm） */
    private static final String KEY_WORK_END = "hr.attendance.work_end_time";
    /** sys_config 键：迟到宽容分钟 */
    private static final String KEY_LATE_GRACE = "hr.attendance.late_grace_minutes";

    private static final LocalTime DEFAULT_WORK_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_WORK_END = LocalTime.of(18, 0);

    private final ISysConfigService configService;

    public AttClockRecordServiceImpl(ISysConfigService configService) {
        this.configService = configService;
    }

    @Override
    public AttClockRecord clockIn(Long userId, String userName, LocalDateTime now) {
        requireLogin(userId);
        LocalDate date = now.toLocalDate();
        AttClockRecord existing = getTodayRecord(userId, date);
        if (existing != null) {
            throw new BusinessException("今日已打卡上班，请勿重复打卡");
        }

        LocalTime workStart = parseRuleTime(KEY_WORK_START, DEFAULT_WORK_START);
        int grace = parseRuleInt(KEY_LATE_GRACE, 0);
        int late = computeLateMinutes(now.toLocalTime(), workStart, grace);

        AttClockRecord record = new AttClockRecord();
        record.setUserId(userId);
        record.setUserName(userName);
        record.setClockDate(date);
        record.setClockInTime(now);
        record.setLateMinutes(late);
        record.setEarlyMinutes(0);
        record.setStatus(late > 0 ? "late" : "normal");
        if (!save(record)) {
            throw new BusinessException("打卡失败");
        }
        return record;
    }

    @Override
    public AttClockRecord clockOut(Long userId, String userName, LocalDateTime now) {
        requireLogin(userId);
        LocalDate date = now.toLocalDate();
        AttClockRecord existing = getTodayRecord(userId, date);
        if (existing == null) {
            throw new BusinessException("请先打卡上班");
        }
        if (existing.getClockOutTime() != null) {
            throw new BusinessException("今日已打卡下班，请勿重复打卡");
        }

        LocalTime workEnd = parseRuleTime(KEY_WORK_END, DEFAULT_WORK_END);
        int early = computeEarlyMinutes(now.toLocalTime(), workEnd);
        String status = existing.getStatus();
        if (early > 0) {
            // 迟到且早退 → 异常；仅早退 → early
            status = "late".equals(status) ? "abnormal" : "early";
        }

        // 原子条件更新：clock_out_time IS NULL，并发重复下班打卡仅首个生效
        boolean ok = update(new LambdaUpdateWrapper<AttClockRecord>()
                .eq(AttClockRecord::getId, existing.getId())
                .isNull(AttClockRecord::getClockOutTime)
                .set(AttClockRecord::getClockOutTime, now)
                .set(AttClockRecord::getEarlyMinutes, early)
                .set(AttClockRecord::getStatus, status));
        if (!ok) {
            throw new BusinessException("打卡状态已变化，请刷新后重试");
        }
        existing.setClockOutTime(now);
        existing.setEarlyMinutes(early);
        existing.setStatus(status);
        return existing;
    }

    @Override
    public List<AttClockRecord> listMonth(Long userId, YearMonth month) {
        return list(new LambdaQueryWrapper<AttClockRecord>()
                .eq(AttClockRecord::getUserId, userId)
                .between(AttClockRecord::getClockDate, month.atDay(1), month.atEndOfMonth())
                .orderByAsc(AttClockRecord::getClockDate));
    }

    @Override
    public AttClockRecord today(Long userId, LocalDateTime now) {
        return getTodayRecord(userId, now.toLocalDate());
    }

    @Override
    public IPage<AttClockMonthlyStat> pageMonthlyStat(long pageNum, long pageSize,
                                                      YearMonth month, String userName, Long deptId) {
        Page<AttClockMonthlyStat> page = new Page<>(pageNum, pageSize);
        return baseMapper.pageMonthlyStat(page, month.atDay(1), month.atEndOfMonth(), userName, deptId);
    }

    // ==================== 内部方法 ====================

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new BusinessException("未登录");
        }
    }

    /** 查询用户指定日期的打卡记录（逻辑删除自动过滤） */
    private AttClockRecord getTodayRecord(Long userId, LocalDate date) {
        return getOne(new LambdaQueryWrapper<AttClockRecord>()
                .eq(AttClockRecord::getUserId, userId)
                .eq(AttClockRecord::getClockDate, date));
    }

    /** 迟到分钟 = 实际打卡时间 - (上班时间 + 宽容分钟)，未超过为 0 */
    private int computeLateMinutes(LocalTime clockTime, LocalTime workStart, int grace) {
        LocalTime limit = workStart.plusMinutes(grace);
        if (clockTime.isAfter(limit)) {
            return (int) Duration.between(limit, clockTime).toMinutes();
        }
        return 0;
    }

    /** 早退分钟 = 下班时间 - 实际打卡时间，未提前为 0 */
    private int computeEarlyMinutes(LocalTime clockTime, LocalTime workEnd) {
        if (clockTime.isBefore(workEnd)) {
            return (int) Duration.between(clockTime, workEnd).toMinutes();
        }
        return 0;
    }

    /** 读取 HH:mm 格式规则，非法或缺失回退默认值 */
    private LocalTime parseRuleTime(String key, LocalTime def) {
        try {
            String v = configService.selectConfigByKey(key);
            if (v == null || v.isBlank()) {
                return def;
            }
            return LocalTime.parse(v.trim(), HHMM);
        } catch (Exception e) {
            return def;
        }
    }

    /** 读取整数规则，非法或缺失回退默认值 */
    private int parseRuleInt(String key, int def) {
        try {
            String v = configService.selectConfigByKey(key);
            if (v == null || v.isBlank()) {
                return def;
            }
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return def;
        }
    }
}