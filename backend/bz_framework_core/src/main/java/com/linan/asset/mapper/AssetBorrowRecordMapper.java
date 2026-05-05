package com.linan.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.asset.entity.AssetBorrowRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产借用记录 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface AssetBorrowRecordMapper extends BaseMapper<AssetBorrowRecord> {
}