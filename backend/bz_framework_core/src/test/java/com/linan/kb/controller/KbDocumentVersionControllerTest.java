package com.linan.kb.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.kb.entity.KbDocument;
import com.linan.kb.entity.KbDocumentVersion;
import com.linan.kb.service.IKbDocumentService;
import com.linan.kb.service.IKbDocumentVersionService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识文档版本快照 Controller 单元测试（2.3）
 *
 * <p>覆盖：新增首版生成 v1、更新前写旧内容快照且 version_no 递增、
 * 更新不存在文档拒绝、版本历史查询倒序。</p>
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
class KbDocumentVersionControllerTest {

    @Mock
    private IKbDocumentService documentService;

    @Mock
    private IKbDocumentVersionService versionService;

    @Mock
    private com.linan.core.security.PermissionService permissionService;

    @InjectMocks
    private KbDocumentController controller;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), KbDocument.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), KbDocumentVersion.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        setThreadLocal("currentUsernameHolder", "admin");
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    private KbDocument doc(Long id, String title, Long categoryId, String content) {
        KbDocument d = new KbDocument();
        d.setId(id);
        d.setTitle(title);
        d.setCategoryId(categoryId);
        d.setContent(content);
        d.setStatus("1");
        return d;
    }

    @Test
    @DisplayName("新增文档：保存后生成首版 v1 快照")
    void save_generatesV1Version() {
        KbDocument d = doc(10L, "测试文档", 2L, "正文v1");
        when(documentService.save(any(KbDocument.class))).thenReturn(true);
        // 无历史版本：version_no=1
        when(versionService.list(any(Wrapper.class))).thenReturn(List.of());

        Result<Void> result = controller.save(d);

        assertEquals(200, result.getCode());
        ArgumentCaptor<KbDocumentVersion> captor = ArgumentCaptor.forClass(KbDocumentVersion.class);
        verify(versionService).save(captor.capture());
        KbDocumentVersion v = captor.getValue();
        assertEquals(10L, v.getDocId());
        assertEquals(1, v.getVersionNo(), "首版版本号为 1");
        assertEquals("测试文档", v.getTitle());
        assertEquals(2L, v.getCategoryId());
        assertEquals("正文v1", v.getContent());
        assertEquals(1L, v.getOperatorId());
    }

    @Test
    @DisplayName("更新文档：写旧内容快照，version_no 在现有基础上递增")
    void update_writesSnapshotBeforeUpdate() {
        KbDocument old = doc(10L, "旧标题", 2L, "旧正文");
        KbDocument newDoc = doc(10L, "新标题", 3L, "新正文");
        when(documentService.getById(10L)).thenReturn(old);
        // 已有 v1：下一个版本号为 2
        KbDocumentVersion v1 = new KbDocumentVersion();
        v1.setDocId(10L);
        v1.setVersionNo(1);
        when(versionService.list(any(Wrapper.class))).thenReturn(List.of(v1));
        when(documentService.updateById(any(KbDocument.class))).thenReturn(true);

        Result<Void> result = controller.update(newDoc);

        assertEquals(200, result.getCode());
        ArgumentCaptor<KbDocumentVersion> captor = ArgumentCaptor.forClass(KbDocumentVersion.class);
        verify(versionService).save(captor.capture());
        KbDocumentVersion v = captor.getValue();
        assertEquals(2, v.getVersionNo(), "快照版本号应在现有基础上递增");
        // 快照内容是旧文档内容（更新前状态）
        assertEquals("旧标题", v.getTitle());
        assertEquals("旧正文", v.getContent());
    }

    @Test
    @DisplayName("更新不存在的文档拒绝，不写快照")
    void update_missingDocument_rejected() {
        KbDocument newDoc = doc(99L, "新标题", 3L, "新正文");
        when(documentService.getById(99L)).thenReturn(null);

        Result<Void> result = controller.update(newDoc);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不存在"));
        verify(versionService, never()).save(any());
        verify(documentService, never()).updateById(any());
    }

    @Test
    @DisplayName("版本历史：按 docId 过滤且 version_no 倒序")
    void versions_orderedDesc() {
        when(versionService.list(any(Wrapper.class))).thenReturn(List.of(
                version(10L, 3), version(10L, 2), version(10L, 1)));

        Result<List<KbDocumentVersion>> result = controller.versions(10L);

        assertEquals(200, result.getCode());
        assertEquals(3, result.getData().size());
        assertEquals(3, result.getData().get(0).getVersionNo());
        assertEquals(1, result.getData().get(2).getVersionNo());
    }

    private KbDocumentVersion version(Long docId, Integer no) {
        KbDocumentVersion v = new KbDocumentVersion();
        v.setDocId(docId);
        v.setVersionNo(no);
        return v;
    }
}