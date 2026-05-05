package com.linan.web.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.linan.core.common.lock.SchedulerLockUtil;
import com.linan.hr.entity.HrSchedule;
import com.linan.hr.service.IHrScheduleService;
import com.linan.web.websocket.NotificationService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 日程提醒调度器
 *
 * <p>每分钟扫描一次「已到提醒时间且未提醒」的日程，通过 WebSocket
 * 向日程所属用户推送提醒，并标记已提醒避免重复推送。</p>
 *
 * <h3>并发安全</h3>
 * <ul>
 *   <li>Redis 分布式锁：多实例部署时同一时刻仅一个实例执行扫描，
 *       避免重复查询与广播（与 {@link com.linan.web.scheduler.SupplyStockWarningScheduler} 一致）</li>
 *   <li>原子条件更新：单条日程仅当 {@code remind_sent='N'} 时置为 'Y'，
 *       即使扫描阶段发生并发，也只有一个执行者能抢占成功并推送</li>
 * </ul>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleReminderScheduler {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 分布式锁 key：防止多实例部署时重复扫描提醒 */
    private static final String REMINDER_LOCK_KEY = "scheduler:lock:schedule_reminder";

    /** 锁持有时间（秒）：任务秒级完成，TTL 兜底防止实例宕机导致死锁 */
    private static final long LOCK_EXPIRE_SECONDS = 30;

    /** 当日去重键前缀：scheduler:timeout_notified:{yyyyMMdd}，仅在线送达才写入，TTL 2 天 */
    private static final String DEDUP_KEY_PREFIX = "scheduler:timeout_notified:";

    private final IHrScheduleService scheduleService;

    private final NotificationService notificationService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final SchedulerLockUtil lockUtil;

    /**
     * 每分钟执行一次日程提醒扫描
     *
     * <p>流程：SchedulerLockUtil 分布式锁（try-finally 释放）→ 查询「未提醒且已到点」清单 →
     * 循环内：当日去重（isMember 跳过）→ 推送（异常不标记，下轮重试）→ 条件更新 N→Y →
     * 仅「标记成功且在线送达」才写入当日去重键。</p>
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void checkDueReminders() {
        // SchedulerLockUtil 分布式锁（SET NX EX + Lua 原子释放）：多实例部署时同一时刻仅一个实例执行扫描
        String token = lockUtil.tryLock(REMINDER_LOCK_KEY, LOCK_EXPIRE_SECONDS);
        if (token == null) {
            log.debug("日程提醒任务已被其他实例抢占，跳过本次执行");
            return;
        }
        try {
            String now = LocalDateTime.now().format(FMT);

            LambdaQueryWrapper<HrSchedule> wrapper = new LambdaQueryWrapper<HrSchedule>()
                    .eq(HrSchedule::getRemindSent, "N")         // 未提醒过
                    .in(HrSchedule::getStatus, "0", "1")        // 未开始/进行中
                    .isNotNull(HrSchedule::getRemindTime)
                    .ne(HrSchedule::getRemindTime, "")
                    .le(HrSchedule::getRemindTime, now);        // 已到提醒时间

            List<HrSchedule> due = scheduleService.list(wrapper);
            if (due.isEmpty()) {
                return;
            }

            String dedupKey = DEDUP_KEY_PREFIX + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            for (HrSchedule schedule : due) {
                if (schedule.getUserId() == null) {
                    continue;
                }
                Long scheduleId = schedule.getId();

                // 当日去重：同一天已在线送达过提醒则跳过（离线未送达不写去重，次日仍可再试）
                Boolean remindedToday = redisTemplate.opsForSet().isMember(dedupKey, scheduleId);
                if (Boolean.TRUE.equals(remindedToday)) {
                    log.debug("日程 {} 当日已提醒过，跳过", scheduleId);
                    continue;
                }

                String title = "日程提醒：" + schedule.getTitle();
                String content = "您有日程即将开始："
                        + schedule.getTitle()
                        + "（" + schedule.getStartTime() + " ~ " + schedule.getEndTime() + "）"
                        + (schedule.getLocation() != null && !schedule.getLocation().isEmpty()
                            ? "，地点：" + schedule.getLocation() : "");

                boolean sent;
                try {
                    sent = notificationService.notifyUser(schedule.getUserId(), title, content);
                } catch (Exception e) {
                    // 推送异常：不标记、不入去重，下轮重试
                    log.warn("日程提醒推送异常，稍后重试: id={}, err={}", scheduleId, e.getMessage());
                    continue;
                }

                // 先推后标记：仅当 remind_sent 仍为 'N' 时置为 'Y'（条件更新，防并发重复推送）
                boolean marked = scheduleService.update(new LambdaUpdateWrapper<HrSchedule>()
                        .eq(HrSchedule::getId, scheduleId)
                        .eq(HrSchedule::getRemindSent, "N")
                        .le(HrSchedule::getRemindTime, now)
                        .set(HrSchedule::getRemindSent, "Y"));

                // 语义：在线送达 → 标记 + 当日去重；离线 → 标记（防死循环）但不入去重
                if (marked && sent) {
                    redisTemplate.opsForSet().add(dedupKey, scheduleId);
                    redisTemplate.expire(dedupKey, 2, TimeUnit.DAYS);
                }
                log.info("日程提醒已推送用户 {}: {}", schedule.getUserId(), schedule.getTitle());
            }
        } finally {
            lockUtil.release(REMINDER_LOCK_KEY, token);
        }
    }
}
