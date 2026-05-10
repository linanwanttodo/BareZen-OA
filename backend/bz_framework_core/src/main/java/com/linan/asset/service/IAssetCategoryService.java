package com.linan.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.asset.entity.AssetCategory;

import java.util.List;

/**
 * 资产分类服务接口
 *
 * @author BareZen Team
 */
public interface IAssetCategoryService extends IService<AssetCategory> {

    /**
     * 查询资产分类列表（可按启用状态过滤）
     *
     * @param status 启用状态（可选），1=启用，0=禁用
     * @return 分类列表
     */
    List<AssetCategory> listCategories(Integer status);

    /**
     * 新增资产分类
     *
     * @param category 分类信息
     * @throws IllegalArgumentException 分类名称为空
     * @throws IllegalStateException 分类名称已存在
     */
    void addCategory(AssetCategory category);

    /**
     * 修改资产分类
     *
     * @param category 分类信息
     * @return 是否修改成功
     */
    boolean updateCategory(AssetCategory category);

    /**
     * 删除资产分类
     *
     * @param id 分类ID
     * @return 是否删除成功
     */
    boolean deleteCategory(Long id);
}