package com.linan.office.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.office.entity.OfficeMeetingBooking;

/**
 * 会议预订服务接口
 *
 * @author BareZen Team
 */
public interface IOfficeMeetingBookingService extends IService<OfficeMeetingBooking> {

    /**
     * 新增会议预订（事务内行锁 + 时间冲突校验，保证并发安全）
     *
     * <p>业务失败抛出 {@link com.linan.core.common.exception.BusinessException}，
     * 由全局异常处理器统一返回 Result，事务自动回滚。</p>
     *
     * @param booking 预订信息（roomId/startTime/endTime 已由 Controller 校验）
     */
    void saveBooking(OfficeMeetingBooking booking);
}
