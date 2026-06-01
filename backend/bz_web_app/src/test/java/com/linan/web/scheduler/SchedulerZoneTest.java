package com.linan.web.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 定时任务时区测试
 *
 * <p>业务定时任务的 cron 必须显式指定业务时区（Asia/Shanghai），不得依赖
 * JVM 默认时区 —— 容器/宿主机时区漂移会导致库存检查、超时扫描在错误时间触发。</p>
 */
class SchedulerZoneTest {

    private static final String BUSINESS_ZONE = "Asia/Shanghai";

    @Test
    @DisplayName("库存预警扫描 cron 显式指定业务时区")
    void supplyStockWarning_cronZoneIsBusinessZone() {
        assertCronZone(SupplyStockWarningScheduler.class, "checkLowStock");
    }

    @Test
    @DisplayName("审批超时扫描 cron 显式指定业务时区")
    void approvalTimeout_cronZoneIsBusinessZone() {
        assertCronZone(ApprovalTimeoutScheduler.class, "checkTimeoutApprovals");
    }

    private void assertCronZone(Class<?> schedulerClass, String methodName) {
        Method method = null;
        for (Method m : schedulerClass.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                method = m;
                break;
            }
        }
        assertNotNull(method, schedulerClass.getSimpleName() + " 缺少方法 " + methodName);

        org.springframework.scheduling.annotation.Scheduled scheduled =
                method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
        assertNotNull(scheduled, method.getName() + " 缺少 @Scheduled 注解");

        assertEquals(BUSINESS_ZONE, scheduled.zone(),
                schedulerClass.getSimpleName() + "." + methodName
                        + " 的 cron 必须显式指定 zone=" + BUSINESS_ZONE
                        + "（业务时区不得依赖 JVM 默认时区）");
    }
}
