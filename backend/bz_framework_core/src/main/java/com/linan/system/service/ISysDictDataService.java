package com.linan.system.service;

import com.linan.system.entity.SysDictData;

import java.util.List;

/**
 * 字典数据 Service 接口
 */
public interface ISysDictDataService {

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
}
