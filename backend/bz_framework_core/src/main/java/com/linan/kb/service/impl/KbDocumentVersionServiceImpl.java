package com.linan.kb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.kb.entity.KbDocumentVersion;
import com.linan.kb.mapper.KbDocumentVersionMapper;
import com.linan.kb.service.IKbDocumentVersionService;
import org.springframework.stereotype.Service;

/**
 * 知识文档版本服务实现
 *
 * @author BareZen Team
 */
@Service
public class KbDocumentVersionServiceImpl
        extends ServiceImpl<KbDocumentVersionMapper, KbDocumentVersion>
        implements IKbDocumentVersionService {
}