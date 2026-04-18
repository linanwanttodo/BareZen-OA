package com.linan.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.article.entity.ArtCategory;

import java.util.List;

public interface IArtCategoryService extends IService<ArtCategory> {

    List<ArtCategory> listAll();

    List<ArtCategory> listEnabled();
}
