package com.linan.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.article.entity.ArtCategory;
import com.linan.article.mapper.ArtCategoryMapper;
import com.linan.article.service.IArtCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArtCategoryServiceImpl extends ServiceImpl<ArtCategoryMapper, ArtCategory> implements IArtCategoryService {

    @Override
    public List<ArtCategory> listAll() {
        return baseMapper.selectList(
                new LambdaQueryWrapper<ArtCategory>()
                        .orderByAsc(ArtCategory::getOrderNum)
        );
    }

    @Override
    public List<ArtCategory> listEnabled() {
        return baseMapper.selectList(
                new LambdaQueryWrapper<ArtCategory>()
                        .eq(ArtCategory::getStatus, 1)
                        .orderByAsc(ArtCategory::getOrderNum)
        );
    }
}
