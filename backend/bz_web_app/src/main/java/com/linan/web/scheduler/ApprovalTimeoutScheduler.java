package com.linan.web.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.hr.service.IAttOvertimeRequestService;
import com.linan.hr.service.IAttTravelRequestService;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeVehicleRequestService;
import com.linan.core.common.lock.SchedulerLockUtil;
import com.linan.system.service.ISysConfigService;
import com.linan.web.websocket.NotificationService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * 审批待办超时提醒调度器（SLA 治理）
 *
 * <p>每天 09:30 扫描各审批流中超过 {@code approval.timeout_hours}（默认 48）小时
 * 仍处于待审批（status=0）的单据，通过 WebSocket 广播催办提醒，减少"待办爆炸"
 * 与流程阻塞——对应大型企业 OA 的待办 SLA/超时治理需求。</p>
 *
 * <p>仅统计数量并广播，不自动处理单据；阈值可通过系统配置
 * {@code approval.timeout_hours} 调整。Redis 分布式锁防多实例重复广播。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalTimeoutScheduler {

    /** 系统配置键：审批超时阈值（小时） */
    private static final String KEY_TIMEOUT_HOURS = "approval.timeout_hours";

    /** 默认超时阈值（小时）：48 小时 */
    private static final int DEFAULT_TIMEOUT_HOURS = 48;

    /** 分布式锁 key：防止多实例部署时重复广播提醒 */
    private static final String TIMEOUT_LOCK_KEY = "scheduler:lock:approval_timeout";

    /** 锁持有时间（秒）：任务通常秒级完成，TTL 兜底防实例宕机死锁；300s 防止慢查询下锁提前过期导致多实例并发广播 */
    private static final long LOCK_EXPIRE_SECONDS = 300;

    private final IAttLeaveRequestService leaveService;

    private final IAttOvertimeRequestService overtimeService;

    private final IAttTravelRequestService travelService;

    private final IOfficeVehicleRequestService vehicleService;

    private final IOfficeSupplyRequestService supplyRequestService;

    private final ISysConfigService configService;

    private final NotificationService notificationService;

    private final SchedulerLockUtil lockUtil;

    /**
     * 每天 09:30 执行待办超时扫描
     */
    @Scheduled(cron = "0 30 9 * * ?", zone = "Asia/Shanghai")
    public void checkTimeoutApprovals() {
        // SchedulerLockUtil 分布式锁（SET NX EX + Lua 原子释放）：多实例部署时同一时刻仅一个实例执行扫描
        String token = lockUtil.tryLock(TIMEOUT_LOCK_KEY, LOCK_EXPIRE_SECONDS);
        if (token == null) {
            log.debug("审批超时提醒任务已被其他实例抢占，跳过本次执行");
            return;
        }
        try {
            int timeoutHours = readTimeoutHours();
            LocalDateTime deadline = LocalDateTime.now().minusHours(timeoutHours);

            // 各审批流待审批超时数量
            List<String> items = new ArrayList<>();
            addIfOverdue(items, "请假", leaveService.count(new LambdaQueryWrapper<AttLeaveRequest>()
                    .eq(AttLeaveRequest::getStatus, "0").lt(AttLeaveRequest::getCreateTime, deadline)));
            addIfOverdue(items, "加班", overtimeService.count(new LambdaQueryWrapper<AttOvertimeRequest>()
                    .eq(AttOvertimeRequest::getStatus, "0").lt(AttOvertimeRequest::getCreateTime, deadline)));
            addIfOverdue(items, "出差", travelService.count(new LambdaQueryWrapper<AttTravelRequest>()
                    .eq(AttTravelRequest::getStatus, "0").lt(AttTravelRequest::getCreateTime, deadline)));
            addIfOverdue(items, "用车", vehicleService.count(new LambdaQueryWrapper<OfficeVehicleRequest>()
                    .eq(OfficeVehicleRequest::getStatus, "0").lt(OfficeVehicleRequest::getCreateTime, deadline)));
            addIfOverdue(items, "用品申领", supplyRequestService.count(new LambdaQueryWrapper<OfficeSupplyRequest>()
                    .eq(OfficeSupplyRequest::getStatus, "0").lt(OfficeSupplyRequest::getCreateTime, deadline)));

            if (items.isEmpty()) {
                return;
            }

            String detail = String.join("、", items);
            String title = "审批超时提醒：" + items.size() + " 类待办积压超 " + timeoutHours + " 小时";
            String content = "以下审批流程有待办超过 " + timeoutHours + " 小时未处理，请及时审批：" + detail;

            notificationService.broadcast(title, content);
            log.info("审批超时提醒已广播：{}", content);
        } finally {
            lockUtil.release(TIMEOUT_LOCK_KEY, token);
        }
    }

    /**
     * 汇总各流程超时数量（仅统计有超时的流程）
     */
    private void addIfOverdue(List<String> items, String type, long count) {
        if (count > 0) {
            items.add(type + " " + count + " 条");
        }
    }

    /**
     * 读取超时阈值（小时）（优先取系统配置，异常/未配置回退默认值）
     */
    private int readTimeoutHours() {
        try {
            String value = configService.selectConfigByKey(KEY_TIMEOUT_HOURS);
            if (value != null && !value.isBlank()) {
                return Integer.parseInt(value.trim());
            }
        } catch (Exception e) {
            log.warn("读取审批超时阈值失败，使用默认值 {} 小时", DEFAULT_TIMEOUT_HOURS, e);
        }
        return DEFAULT_TIMEOUT_HOURS;
    }
}
