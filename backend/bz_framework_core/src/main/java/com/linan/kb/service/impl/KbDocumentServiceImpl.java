package com.linan.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.PermissionService;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.kb.entity.KbDocument;
import com.linan.kb.entity.KbDocumentVersion;
import com.linan.kb.mapper.KbDocumentMapper;
import com.linan.kb.service.IKbDocumentService;
import com.linan.kb.service.IKbDocumentVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识文档服务实现
 *
 * <p>集中承载版本快照、作者兜底、浏览量自增、删除双轨校验等业务规则，
 * 使 Controller 退化为参数绑定与 Result 委托。</p>
 */
@Service
@RequiredArgsConstructor
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument>
        implements IKbDocumentService {

    private final IKbDocumentVersionService versionService;
    private final PermissionService permissionService;

    /** 文档状态：草稿（新建文档的默认状态，1=已发布） */
    private static final String STATUS_DRAFT = "0";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createDocument(KbDocument doc) {
        if (doc.getStatus() == null || doc.getStatus().isEmpty()) {
            doc.setStatus(STATUS_DRAFT);
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        doc.setAuthorId(userId);
        if (doc.getAuthorName() == null || doc.getAuthorName().isEmpty()) {
            // 后端兜底填真实用户名（JWT 中解析），禁止把 userId 当姓名落库
            doc.setAuthorName(userId != null ? AuthInterceptor.getCurrentUsername() : null);
        }
        if (doc.getViewCount() == null) {
            doc.setViewCount(0);
        }
        boolean ok = save(doc);
        if (ok) {
            writeVersion(doc, userId);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDocument(KbDocument doc) {
        KbDocument old = getById(doc.getId());
        if (old == null) {
            throw new BusinessException("文档不存在");
        }
        // 更新前快照：写入旧内容副本（operator 为当前用户）
        Long userId = AuthInterceptor.getCurrentUserId();
        writeVersion(old, userId);
        return updateById(doc);
    }

    @Override
    public boolean deleteDocument(Long id, Long userId) {
        KbDocument doc = getById(id);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }
        boolean isOwner = userId.equals(doc.getAuthorId());
        boolean isAdmin = permissionService.hasPermission(userId, "kb:document:delete");
        if (!isOwner && !isAdmin) {
            throw new BusinessException("无权限删除该文档");
        }
        return removeById(id);
    }

    @Override
    public KbDocument viewDocument(Long id) {
        KbDocument doc = getById(id);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }
        // 浏览量 +1（幂等，仅统计）
        lambdaUpdate()
                .eq(KbDocument::getId, id)
                .setSql("view_count = view_count + 1")
                .update();
        doc.setViewCount(doc.getViewCount() != null ? doc.getViewCount() + 1 : 1);
        return doc;
    }

    /**
     * 写版本快照：version_no = 现有最大 + 1（首版为 1）
     */
    private void writeVersion(KbDocument doc, Long operatorId) {
        KbDocumentVersion version = new KbDocumentVersion();
        version.setDocId(doc.getId());
        version.setTitle(doc.getTitle());
        version.setCategoryId(doc.getCategoryId());
        version.setContent(doc.getContent());
        version.setOperatorId(operatorId);
        version.setOperatorName(operatorId != null ? AuthInterceptor.getCurrentUsername() : null);
        version.setVersionNo(nextVersionNo(doc.getId()));
        versionService.save(version);
    }

    /** 文档当前最大版本号 + 1（无历史时从 1 开始） */
    private int nextVersionNo(Long docId) {
        List<KbDocumentVersion> versions = versionService.list(new LambdaQueryWrapper<KbDocumentVersion>()
                .eq(KbDocumentVersion::getDocId, docId)
                .orderByDesc(KbDocumentVersion::getVersionNo)
                .last("LIMIT 1"));
        return versions.isEmpty() ? 1 : versions.get(0).getVersionNo() + 1;
    }
}