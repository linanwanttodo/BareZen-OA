package com.linan.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.asset.entity.AssetBorrowRecord;
import com.linan.asset.entity.AssetItem;
import com.linan.asset.mapper.AssetItemMapper;
import com.linan.asset.service.IAssetBorrowRecordService;
import com.linan.asset.service.IAssetItemService;
import com.linan.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 资产台账服务实现
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class AssetItemServiceImpl extends ServiceImpl<AssetItemMapper, AssetItem>
        implements IAssetItemService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IAssetBorrowRecordService borrowRecordService;

    @Override
    public List<AssetItem> listByCategoryId(Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<AssetItem>()
                .eq(AssetItem::getCategoryId, categoryId)
                .orderByAsc(AssetItem::getAssetName));
    }

    @Override
    public List<AssetItem> listAvailableAssets() {
        // 假设资产状态：0=正常，1=借用中，2=维修中，3=报废
        return list(new LambdaQueryWrapper<AssetItem>()
                .eq(AssetItem::getStatus, "0")
                .orderByAsc(AssetItem::getAssetName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAssetStatus(Long assetId, String status) {
        if (assetId == null) {
            throw new BusinessException("资产ID不能为空");
        }
        if (status == null || status.isBlank()) {
            throw new BusinessException("状态不能为空");
        }
        return update(new LambdaUpdateWrapper<AssetItem>()
                .eq(AssetItem::getId, assetId)
                .set(AssetItem::getStatus, status));
    }

    @Override
    public boolean isAssetNameUnique(String assetName, Long excludeId) {
        if (assetName == null || assetName.isBlank()) {
            return false;
        }
        
        LambdaQueryWrapper<AssetItem> wrapper = new LambdaQueryWrapper<AssetItem>()
                .eq(AssetItem::getAssetName, assetName);
        
        if (excludeId != null) {
            wrapper.ne(AssetItem::getId, excludeId);
        }
        
        long count = count(wrapper);
        return count == 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void borrowAsset(Long id, String borrowerName, String expectReturnTime, String remark) {
        borrowAsset(id, null, borrowerName, expectReturnTime, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void borrowAsset(Long id, Long borrowerId, String borrowerName, String expectReturnTime, String remark) {
        AssetItem item = getById(id);
        if (item == null) {
            throw new BusinessException("资产不存在");
        }
        if (!"0".equals(item.getStatus())) {
            throw new BusinessException("仅限在库资产借用（当前状态：" + item.getStatus() + "）");
        }
        // 在库状态原子流转为使用中，防止并发重复借用
        boolean ok = update(new LambdaUpdateWrapper<AssetItem>()
                .eq(AssetItem::getId, id)
                .eq(AssetItem::getStatus, "0")
                .set(AssetItem::getStatus, "1")
                .set(AssetItem::getHolderName, borrowerName));
        if (!ok) {
            throw new BusinessException("该资产已被并发借用，请刷新后重试");
        }
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setAssetId(id);
        record.setBorrowerId(borrowerId);
        record.setBorrowerName(borrowerName);
        record.setBorrowTime(LocalDateTime.now().format(FMT));
        record.setExpectReturnTime(expectReturnTime);
        record.setRemark(remark);
        record.setStatus("0");
        borrowRecordService.save(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnAsset(Long id) {
        AssetItem item = getById(id);
        if (item == null) {
            throw new BusinessException("资产不存在");
        }
        if (!"1".equals(item.getStatus())) {
            throw new BusinessException("仅使用中的资产可归还");
        }
        boolean ok = update(new LambdaUpdateWrapper<AssetItem>()
                .eq(AssetItem::getId, id)
                .eq(AssetItem::getStatus, "1")
                .set(AssetItem::getStatus, "0")
                .set(AssetItem::getHolderName, (String) null));
        if (!ok) {
            throw new BusinessException("该资产状态已变更，归还失败");
        }
        // 最新借用中记录置为已归还
        borrowRecordService.update(new LambdaUpdateWrapper<AssetBorrowRecord>()
                .eq(AssetBorrowRecord::getAssetId, id)
                .eq(AssetBorrowRecord::getStatus, "0")
                .set(AssetBorrowRecord::getStatus, "1")
                .set(AssetBorrowRecord::getReturnTime, LocalDateTime.now().format(FMT)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scrapAsset(Long id) {
        AssetItem item = getById(id);
        if (item == null) {
            throw new BusinessException("资产不存在");
        }
        if ("3".equals(item.getStatus())) {
            throw new BusinessException("该资产已报废");
        }
        item.setStatus("3");
        item.setScrapDate(LocalDate.now().toString());
        boolean ok = updateById(item);
        if (!ok) {
            throw new BusinessException("报废失败");
        }
    }
}
