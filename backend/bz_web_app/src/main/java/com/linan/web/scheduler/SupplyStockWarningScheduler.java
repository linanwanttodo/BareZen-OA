package com.linan.web.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.service.IOfficeSupplyService;
import com.linan.system.service.ISysConfigService;
import com.linan.web.websocket.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 办公用品库存预警调度器
 *
 * <p>每天 09:00 检查库存低于阈值的用品，通过 WebSocket 广播提醒管理员补货。
 * 阈值可通过系统配置 {@code office.supply.stock_warn_threshold} 调整（默认 5）。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class SupplyStockWarningScheduler {

    /** 系统配置键：库存预警阈值 */
    private static final String KEY_WARN_THRESHOLD = "office.supply.stock_warn_threshold";

    /** 默认预警阈值 */
    private static final int DEFAULT_THRESHOLD = 5;

    @Autowired
    private IOfficeSupplyService supplyService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 每天 09:00 执行库存检查
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkLowStock() {
        int threshold = readThreshold();
        LambdaQueryWrapper<OfficeSupply> wrapper = new LambdaQueryWrapper<OfficeSupply>()
                .eq(OfficeSupply::getStatus, "0")
                .le(OfficeSupply::getStock, threshold)
                .orderByAsc(OfficeSupply::getStock);

        List<OfficeSupply> lowStock = supplyService.list(wrapper);
        if (lowStock.isEmpty()) {
            return;
        }

        String detail = lowStock.stream()
                .map(s -> s.getSupplyName() + "（剩余 " + s.getStock() + " " + (s.getUnit() != null ? s.getUnit() : "个") + "）")
                .collect(Collectors.joining("、"));
        String title = "库存预警：" + lowStock.size() + " 种用品库存不足";
        String content = "以下用品库存低于阈值（" + threshold + "），请及时补货：" + detail;

        notificationService.broadcast(title, content);
        log.info("库存预警已广播：{}", content);
    }

    /**
     * 读取预警阈值（优先取系统配置，异常/未配置回退默认值）
     */
    private int readThreshold() {
        try {
            String value = configService.selectConfigByKey(KEY_WARN_THRESHOLD);
            if (value != null && !value.isBlank()) {
                return Integer.parseInt(value.trim());
            }
        } catch (Exception e) {
            log.warn("读取库存预警阈值失败，使用默认值 {}", DEFAULT_THRESHOLD, e);
        }
        return DEFAULT_THRESHOLD;
    }
}
