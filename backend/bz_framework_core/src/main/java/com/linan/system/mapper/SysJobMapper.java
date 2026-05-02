package com.linan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.system.entity.SysJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务 Mapper 接口
 *
 * 继承 BaseMapper<SysJob> 自动获得 CRUD 方法。
 *
 * @author BareZen Team
 */
@Mapper
public interface SysJobMapper extends BaseMapper<SysJob> {
}
