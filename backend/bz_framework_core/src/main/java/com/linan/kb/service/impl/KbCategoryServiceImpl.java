package com.linan.kb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.kb.entity.KbCategory;
import com.linan.kb.mapper.KbCategoryMapper;
import com.linan.kb.service.IKbCategoryService;
import org.springframework.stereotype.Service;

/**
 * 知识库分类服务实现
 *
 * @author BareZen Team
 */
@Service
public class KbCategoryServiceImpl
        extends ServiceImpl<KbCategoryMapper, KbCategory>
        implements IKbCategoryService {
}