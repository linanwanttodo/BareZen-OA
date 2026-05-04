package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysDictData;
import com.linan.system.mapper.SysDictDataMapper;
import com.linan.system.service.ISysDictDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典数据 Service 实现类
 *
 * <p>使用 Spring Cache（Redis）缓存字典查询结果，减少数据库压力。
 * 缓存名称为 {@code dict_data}，TTL 默认 300 秒（5 分钟）。</p>
 */
@Slf4j
@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements ISysDictDataService {

    @Override
    @Cacheable(value = "dict_data", key = "#dictType", unless = "#result == null or #result.isEmpty()")
    public List<SysDictData> selectDictDataByType(String dictType) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        // 状态：0=停用 1=正常，只查启用项
        wrapper.eq(SysDictData::getDictType, dictType)
               .eq(SysDictData::getStatus, "1")
               .orderByAsc(SysDictData::getDictSort);

        return list(wrapper);
    }

    @Override
    @CacheEvict(value = "dict_data", key = "#dictType")
    public void clearDictCache(String dictType) {
        log.debug("已清除字典缓存: {}", dictType);
    }

    @Override
    @CacheEvict(value = "dict_data", allEntries = true)
    public void clearAllDictCache() {
        log.debug("已清除全部字典缓存");
    }
}
