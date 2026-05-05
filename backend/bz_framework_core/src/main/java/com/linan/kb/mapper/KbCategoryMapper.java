package com.linan.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.kb.entity.KbCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库分类 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface KbCategoryMapper extends BaseMapper<KbCategory> {
}