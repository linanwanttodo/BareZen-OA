package com.linan.asset.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.asset.entity.AssetBorrowRecord;
import com.linan.asset.mapper.AssetBorrowRecordMapper;
import com.linan.asset.service.IAssetBorrowRecordService;
import org.springframework.stereotype.Service;

/**
 * 资产借用记录服务实现
 *
 * @author BareZen Team
 */
@Service
public class AssetBorrowRecordServiceImpl
        extends ServiceImpl<AssetBorrowRecordMapper, AssetBorrowRecord>
        implements IAssetBorrowRecordService {
}