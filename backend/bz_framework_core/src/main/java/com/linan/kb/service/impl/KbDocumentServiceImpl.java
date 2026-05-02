package com.linan.kb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.kb.entity.KbDocument;
import com.linan.kb.mapper.KbDocumentMapper;
import com.linan.kb.service.IKbDocumentService;
import org.springframework.stereotype.Service;

/**
 * 知识文档服务实现
 *
 * @author BareZen Team
 */
@Service
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument>
        implements IKbDocumentService {
}
