package com.linan.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.linan.hr.domain.AttClockMonthlyStat;
import com.linan.hr.entity.AttClockRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 考勤打卡记录 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface AttClockRecordMapper extends BaseMapper<AttClockRecord> {

    /**
     * 月度考勤统计（按员工分组，分页）
     *
     * @param page      分页对象（MyBatis-Plus 分页插件自动处理 LIMIT）
     * @param monthStart 月份起始日期
     * @param monthEnd   月份结束日期
     * @param userName   员工姓名模糊筛选（可选）
     * @param deptId     部门ID筛选（可选）
     */
    IPage<AttClockMonthlyStat> pageMonthlyStat(IPage<?> page,
                                               @Param("monthStart") LocalDate monthStart,
                                               @Param("monthEnd") LocalDate monthEnd,
                                               @Param("userName") String userName,
                                               @Param("deptId") Long deptId);
}