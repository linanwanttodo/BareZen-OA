package com.linan.asset.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.asset.entity.AssetItem;
import com.linan.asset.mapper.AssetItemMapper;
import com.linan.asset.service.IAssetItemService;
import org.springframework.stereotype.Service;

/**
 * 资产台账服务实现
 *
 * @author BareZen Team
 */
@Service
public class AssetItemServiceImpl extends ServiceImpl<AssetItemMapper, AssetItem>
        implements IAssetItemService {
}
