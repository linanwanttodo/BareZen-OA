package com.linan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.system.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务执行日志 Mapper 接口
 *
 * 继承 BaseMapper<SysJobLog> 自动获得 CRUD 方法。
 *
 * @author BareZen Team
 */
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {
}
