package com.linan.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.asset.entity.AssetItem;
import com.linan.asset.mapper.AssetItemMapper;
import com.linan.asset.service.IAssetItemService;
import com.linan.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
