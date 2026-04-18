package com.linan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.system.entity.SysNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNotice> {

    @Select("SELECT * FROM sys_notice " +
            "WHERE notice_type = '1' " +
            "AND status = '1' " +
            "AND del_flag = 0 " +
            "ORDER BY create_time DESC " +
            "LIMIT 1")
    SysNotice selectLatestNotice();

    @Select("SELECT * FROM sys_notice " +
            "WHERE notice_type = '2' " +
            "AND status = '1' " +
            "AND del_flag = 0 " +
            "ORDER BY create_time DESC " +
            "LIMIT #{limit}")
    List<SysNotice> selectPublishedNotices(@Param("limit") int limit);
}