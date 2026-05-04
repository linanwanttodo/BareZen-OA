package com.linan.office.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.office.entity.OfficeSupplyRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用品申领记录 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface OfficeSupplyRequestMapper extends BaseMapper<OfficeSupplyRequest> {

    /**
     * 悲观锁查询申领记录（SELECT ... FOR UPDATE）
     *
     * <p>审批时在事务内先锁定申领记录行，使同一记录的并发审批串行化，
     * 后到者等待锁释放后能看到最新状态，避免重复审批导致重复扣库存。</p>
     */
    @Select("SELECT * FROM office_supply_request WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    OfficeSupplyRequest selectByIdForUpdate(@Param("id") Long id);
}
