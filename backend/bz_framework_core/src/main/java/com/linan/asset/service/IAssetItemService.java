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

    /**
     * 借用资产（仅在库可借；借用中不可重复借）
     *
     * <p>管理端口径：借用记录仅记借用人姓名，不关联用户ID。</p>
     *
     * @param id               资产ID
     * @param borrowerName     借用人姓名
     * @param expectReturnTime 预计归还时间
     * @param remark           备注
     */
    void borrowAsset(Long id, String borrowerName, String expectReturnTime, String remark);

    /**
     * 借用资产并记录借用人用户ID（用户前台口径，服务端强制注入当前登录用户）
     *
     * <p>与 {@link #borrowAsset(Long, String, String, String)} 共用同一套
     * 在库校验与原子状态流转（WHERE status='0'），仅借用记录额外落 borrowerId，
     * 供「我的借用」归属过滤与归还归属校验使用。</p>
     *
     * @param id               资产ID
     * @param borrowerId       借用人用户ID
     * @param borrowerName     借用人姓名
     * @param expectReturnTime 预计归还时间
     * @param remark           备注
     */
    void borrowAsset(Long id, Long borrowerId, String borrowerName, String expectReturnTime, String remark);

    /**
     * 归还资产（使用中才可归还，归还置回在库并记录归还时间）
     *
     * @param id 资产ID
     */
    void returnAsset(Long id);

    /**
     * 报废资产（状态置为已报废并记录报废日期）
     *
     * @param id 资产ID
     */
    void scrapAsset(Long id);
}
