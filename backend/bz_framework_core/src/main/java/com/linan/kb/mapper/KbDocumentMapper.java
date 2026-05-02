package com.linan.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.kb.entity.KbDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识文档 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface KbDocumentMapper extends BaseMapper<KbDocument> {
}
