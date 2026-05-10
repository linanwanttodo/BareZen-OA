package com.linan.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.asset.entity.AssetItem;

import java.util.List;

/**
 * 资产台账服务接口
 *
 * @author BareZen Team
 */
public interface IAssetItemService extends IService<AssetItem> {

    /**
     * 查询指定分类的资产列表
     *
     * @param categoryId 分类ID
     * @return 资产列表
     */
    List<AssetItem> listByCategoryId(Long categoryId);

    /**
     * 查询可用的资产列表（状态为正常且未被借用）
     *
     * @return 可用资产列表
     */
    List<AssetItem> listAvailableAssets();

    /**
     * 更新资产状态
     *
     * @param assetId 资产ID
     * @param status 状态
     * @return 是否更新成功
     */
    boolean updateAssetStatus(Long assetId, String status);

    /**
     * 检查资产名称是否唯一
     *
     * @param assetName 资产名称
     * @param excludeId 排除的资产ID（更新时使用）
     * @return true=唯一，false=不唯一
     */
    boolean isAssetNameUnique(String assetName, Long excludeId);
}
