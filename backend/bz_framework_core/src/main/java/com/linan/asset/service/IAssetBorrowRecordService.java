package com.linan.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.asset.entity.AssetBorrowRecord;

import java.util.List;

/**
 * 资产借用记录服务接口
 *
 * @author BareZen Team
 */
public interface IAssetBorrowRecordService extends IService<AssetBorrowRecord> {

    /**
     * 查询指定资产的借用记录
     *
     * @param assetId 资产ID
     * @return 借用记录列表
     */
    List<AssetBorrowRecord> listByAssetId(Long assetId);

    /**
     * 查询指定用户的借用记录
     *
     * @param borrowerId 借用人ID
     * @return 借用记录列表
     */
    List<AssetBorrowRecord> listByBorrowerId(Long borrowerId);

    /**
     * 查询当前借用中的记录
     *
     * @return 借用中的记录列表
     */
    List<AssetBorrowRecord> listActiveBorrows();

    /**
     * 创建借用记录
     *
     * @param record 借用记录
     * @throws IllegalArgumentException 参数不合法
     */
    void createBorrowRecord(AssetBorrowRecord record);

    /**
     * 归还资产
     *
     * @param id 借用记录ID
     * @return 是否归还成功
     */
    boolean returnAsset(Long id);

    /**
     * 检查资产是否可借用（未被借用中）
     *
     * @param assetId 资产ID
     * @return true=可借用，false=不可借用
     */
    boolean isAssetAvailable(Long assetId);
}