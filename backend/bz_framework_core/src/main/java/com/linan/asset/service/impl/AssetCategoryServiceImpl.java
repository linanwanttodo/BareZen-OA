package com.linan.asset.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.asset.entity.AssetCategory;
import com.linan.asset.mapper.AssetCategoryMapper;
import com.linan.asset.service.IAssetCategoryService;
import org.springframework.stereotype.Service;

/**
 * 资产分类服务实现
 *
 * @author BareZen Team
 */
@Service
public class AssetCategoryServiceImpl
        extends ServiceImpl<AssetCategoryMapper, AssetCategory>
        implements IAssetCategoryService {
}