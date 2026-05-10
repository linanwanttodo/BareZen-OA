package com.linan.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.asset.entity.AssetCategory;
import com.linan.asset.mapper.AssetCategoryMapper;
import com.linan.asset.service.IAssetCategoryService;
import com.linan.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 资产分类服务实现
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class AssetCategoryServiceImpl
        extends ServiceImpl<AssetCategoryMapper, AssetCategory>
        implements IAssetCategoryService {

    @Override
    public List<AssetCategory> listCategories(Integer status) {
        LambdaQueryWrapper<AssetCategory> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AssetCategory::getStatus, status);
        }
        wrapper.orderByAsc(AssetCategory::getSort)
                .orderByAsc(AssetCategory::getId);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCategory(AssetCategory category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new BusinessException("分类名称不能为空");
        }
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        // 同名唯一（uk_name）
        long exists = count(new LambdaQueryWrapper<AssetCategory>()
                .eq(AssetCategory::getName, category.getName()));
        if (exists > 0) {
            throw new BusinessException("分类名称已存在");
        }
        boolean ok = save(category);
        if (!ok) {
            throw new BusinessException("新增失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCategory(AssetCategory category) {
        if (category.getId() == null) {
            throw new BusinessException("分类ID不能为空");
        }
        return updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategory(Long id) {
        return removeById(id);
    }
}