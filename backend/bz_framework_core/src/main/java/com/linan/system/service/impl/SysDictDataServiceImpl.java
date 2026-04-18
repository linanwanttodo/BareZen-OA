package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.system.entity.SysDictData;
import com.linan.system.mapper.SysDictDataMapper;
import com.linan.system.service.ISysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 字典数据 Service 实现类
 */
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

    @Autowired
    private SysDictDataMapper dictDataMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String DICT_CACHE_PREFIX = "sys_dict:";

    @Override
    public List<SysDictData> selectDictDataByType(String dictType) {
        // 1. 先从 Redis 缓存中获取
        String cacheKey = DICT_CACHE_PREFIX + dictType;
        @SuppressWarnings("unchecked")
        List<SysDictData> cachedData = (List<SysDictData>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedData != null && !cachedData.isEmpty()) {
            return cachedData;
        }

        // 2. 缓存中没有，查询数据库
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType)
               .eq(SysDictData::getStatus, "1") // 只查启用的
               .orderByAsc(SysDictData::getDictSort);
        
        List<SysDictData> dbData = dictDataMapper.selectList(wrapper);

        // 3. 存入 Redis 缓存，设置过期时间为 1 小时
        if (dbData != null && !dbData.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, dbData, 1, TimeUnit.HOURS);
        }

        return dbData;
    }

    @Override
    public void clearDictCache(String dictType) {
        String cacheKey = DICT_CACHE_PREFIX + dictType;
        redisTemplate.delete(cacheKey);
    }
}
