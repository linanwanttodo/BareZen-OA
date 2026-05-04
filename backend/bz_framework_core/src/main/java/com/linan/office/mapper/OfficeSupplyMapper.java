package com.linan.office.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.office.entity.OfficeSupply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 办公用品 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface OfficeSupplyMapper extends BaseMapper<OfficeSupply> {

    /**
     * 悲观锁查询用品（SELECT ... FOR UPDATE）
     *
     * <p>审批通过扣减库存时在事务内先锁定用品行，使同一用品的并发扣减串行化，
     * 后到者在锁释放后重新读取库存，避免超扣（丢失更新）。</p>
     */
    @Select("SELECT * FROM office_supply WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    OfficeSupply selectByIdForUpdate(@Param("id") Long id);
}
