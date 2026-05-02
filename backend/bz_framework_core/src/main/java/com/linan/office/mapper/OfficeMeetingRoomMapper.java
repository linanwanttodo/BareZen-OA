package com.linan.office.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.office.entity.OfficeMeetingRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 会议室 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface OfficeMeetingRoomMapper extends BaseMapper<OfficeMeetingRoom> {

    /**
     * 悲观锁查询会议室（SELECT ... FOR UPDATE）
     *
     * <p>会议预订在事务内先锁定会议室行，再执行时间冲突校验与插入，
     * 保证高并发下同一会议室的预订请求串行化，避免重复预订。</p>
     */
    @Select("SELECT * FROM office_meeting_room WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    OfficeMeetingRoom selectByIdForUpdate(@Param("id") Long id);
}
