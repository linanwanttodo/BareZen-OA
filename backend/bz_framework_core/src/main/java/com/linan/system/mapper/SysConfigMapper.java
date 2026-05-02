package com.linan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.system.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 根据配置键名查询配置值
     */
    @Select("SELECT config_value FROM sys_config WHERE config_key = #{configKey} AND status = '1' AND del_flag = 0")
    String selectConfigValueByKey(@Param("configKey") String configKey);
}
