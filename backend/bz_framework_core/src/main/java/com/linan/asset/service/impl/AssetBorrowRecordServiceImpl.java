package com.linan.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.asset.entity.AssetBorrowRecord;
import com.linan.asset.mapper.AssetBorrowRecordMapper;
import com.linan.asset.service.IAssetBorrowRecordService;
import com.linan.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 资产借用记录服务实现
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class AssetBorrowRecordServiceImpl
        extends ServiceImpl<AssetBorrowRecordMapper, AssetBorrowRecord>
        implements IAssetBorrowRecordService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String STATUS_BORROWING = "0";
    private static final String STATUS_RETURNED = "1";

    @Override
    public List<AssetBorrowRecord> listByAssetId(Long assetId) {
        return list(new LambdaQueryWrapper<AssetBorrowRecord>()
                .eq(AssetBorrowRecord::getAssetId, assetId)
                .orderByDesc(AssetBorrowRecord::getBorrowTime));
    }

    @Override
    public List<AssetBorrowRecord> listByBorrowerId(Long borrowerId) {
        return list(new LambdaQueryWrapper<AssetBorrowRecord>()
                .eq(AssetBorrowRecord::getBorrowerId, borrowerId)
                .orderByDesc(AssetBorrowRecord::getBorrowTime));
    }

    @Override
    public List<AssetBorrowRecord> listActiveBorrows() {
        return list(new LambdaQueryWrapper<AssetBorrowRecord>()
                .eq(AssetBorrowRecord::getStatus, STATUS_BORROWING)
                .orderByAsc(AssetBorrowRecord::getExpectReturnTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBorrowRecord(AssetBorrowRecord record) {
        if (record.getAssetId() == null) {
            throw new BusinessException("资产ID不能为空");
        }
        if (record.getBorrowerId() == null) {
            throw new BusinessException("借用人ID不能为空");
        }
        if (record.getBorrowerName() == null || record.getBorrowerName().isBlank()) {
            throw new BusinessException("借用人姓名不能为空");
        }

        // 检查资产是否可借用
        if (!isAssetAvailable(record.getAssetId())) {
            throw new BusinessException("该资产当前正在借用中，无法再次借用");
        }

        // 设置默认值
        if (record.getBorrowTime() == null || record.getBorrowTime().isBlank()) {
            record.setBorrowTime(LocalDateTime.now().format(FMT));
        }
        if (record.getStatus() == null) {
            record.setStatus(STATUS_BORROWING);
        }

        boolean ok = save(record);
        if (!ok) {
            throw new BusinessException("创建借用记录失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean returnAsset(Long id) {
        AssetBorrowRecord record = getById(id);
        if (record == null) {
            throw new BusinessException("借用记录不存在");
        }
        if (STATUS_RETURNED.equals(record.getStatus())) {
            throw new BusinessException("该资产已归还");
        }

        return update(new LambdaUpdateWrapper<AssetBorrowRecord>()
                .eq(AssetBorrowRecord::getId, id)
                .set(AssetBorrowRecord::getStatus, STATUS_RETURNED)
                .set(AssetBorrowRecord::getReturnTime, LocalDateTime.now().format(FMT)));
    }

    @Override
    public boolean isAssetAvailable(Long assetId) {
        long count = count(new LambdaQueryWrapper<AssetBorrowRecord>()
                .eq(AssetBorrowRecord::getAssetId, assetId)
                .eq(AssetBorrowRecord::getStatus, STATUS_BORROWING));
        return count == 0;
    }
}