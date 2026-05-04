package com.linan.web.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.linan.hr.entity.HrSchedule;
import com.linan.hr.service.IHrScheduleService;
import com.linan.web.websocket.NotificationService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日程提醒调度器
 *
 * <p>每分钟扫描一次「已到提醒时间且未提醒」的日程，通过 WebSocket
 * 向日程所属用户推送提醒，并标记已提醒避免重复推送。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleReminderScheduler {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final IHrScheduleService scheduleService;

    private final NotificationService notificationService;

    /**
     * 每分钟执行一次日程提醒扫描
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void checkDueReminders() {
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

        for (HrSchedule schedule : due) {
            if (schedule.getUserId() == null) {
                continue;
            }
            // 原子抢占（条件更新）：仅当 remind_sent 仍为 'N' 时才置为 'Y'。
            // 多实例部署或并发执行时，同一日程只会有一个执行者抢占成功并推送，
            // 其余执行者更新影响 0 行直接跳过，避免重复提醒（原子抢占，P1）
            boolean claimed = scheduleService.update(new LambdaUpdateWrapper<HrSchedule>()
                    .eq(HrSchedule::getId, schedule.getId())
                    .eq(HrSchedule::getRemindSent, "N")
                    .le(HrSchedule::getRemindTime, now)
                    .set(HrSchedule::getRemindSent, "Y"));
            if (!claimed) {
                continue;
            }

            String title = "日程提醒：" + schedule.getTitle();
            String content = "您有日程即将开始："
                    + schedule.getTitle()
                    + "（" + schedule.getStartTime() + " ~ " + schedule.getEndTime() + "）"
                    + (schedule.getLocation() != null && !schedule.getLocation().isEmpty()
                        ? "，地点：" + schedule.getLocation() : "");
            notificationService.notifyUser(schedule.getUserId(), title, content);
            log.info("日程提醒已推送用户 {}: {}", schedule.getUserId(), schedule.getTitle());
        }
    }
}
