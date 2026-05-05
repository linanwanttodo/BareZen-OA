package com.linan.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.hr.domain.AttClockMonthlyStat;
import com.linan.hr.entity.AttClockRecord;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 考勤打卡记录服务
 *
 * @author BareZen Team
 */
public interface IAttClockRecordService extends IService<AttClockRecord> {

    /**
     * 上班打卡
     *
     * @param userId   当前登录用户ID（不信任前端传参）
     * @param userName 当前登录用户名（冗余落库）
     * @param now      打卡时间（由调用方注入便于测试）
     * @return 落库后的打卡记录
     */
    AttClockRecord clockIn(Long userId, String userName, LocalDateTime now);

    /**
     * 下班打卡
     *
     * @param userId   当前登录用户ID
     * @param userName 当前登录用户名
     * @param now      打卡时间
     * @return 更新后的打卡记录
     */
    AttClockRecord clockOut(Long userId, String userName, LocalDateTime now);

    /**
     * 用户当月打卡记录（按日期升序）
     */
    List<AttClockRecord> listMonth(Long userId, YearMonth month);

    /**
     * 用户今日打卡记录（可能为 null）
     */
    AttClockRecord today(Long userId, LocalDateTime now);

    /**
     * 月度考勤统计（按员工分组，分页）
     */
    IPage<AttClockMonthlyStat> pageMonthlyStat(long pageNum, long pageSize,
                                               YearMonth month, String userName, Long deptId);
}