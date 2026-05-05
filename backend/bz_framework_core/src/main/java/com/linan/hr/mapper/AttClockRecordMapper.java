package com.linan.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.hr.entity.AttClockRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤打卡记录 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface AttClockRecordMapper extends BaseMapper<AttClockRecord> {
}