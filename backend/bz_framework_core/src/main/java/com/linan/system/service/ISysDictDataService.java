package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysDictData;

import java.util.List;

/**
 * 字典数据 Service 接口
 */
public interface ISysDictDataService extends IService<SysDictData> {

    /**
     * 根据字典类型查询字典数据列表（带缓存）
     *
     * @param dictType 字典类型
     * @return 字典数据集合
     */
    List<SysDictData> selectDictDataByType(String dictType);

    /**
     * 清空指定字典类型的缓存
     *
     * @param dictType 字典类型
     */
    void clearDictCache(String dictType);

    /**
     * 清空所有字典缓存（管理员在字典管理页面调用）
     */
    void clearAllDictCache();
}
