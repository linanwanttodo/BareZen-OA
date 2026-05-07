package com.linan.kb.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识文档 Controller 单测（2.3）
 *
 * <p>版本快照（续版/首版 v1）规则已下沉至 {@link IKbDocumentService#createDocument} 与
 * {@link IKbDocumentService#updateDocument}，由 {@code KbDocumentServiceImplTest} 覆盖；
 * 此处仅验证 Controller 正确委托服务方法。版本历史倒序查询仍由 Controller 直接委托。</p>
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
class KbDocumentVersionControllerTest {

    @Mock
    private IKbDocumentService documentService;

    @Mock
    private IKbDocumentVersionService versionService;

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
    @DisplayName("新增文档：委托 createDocument，成功返回 200")
    void save_delegatesToCreate() {
        KbDocument d = doc(10L, "测试文档", 2L, "正文v1");
        when(documentService.createDocument(d)).thenReturn(true);

        Result<Void> result = controller.save(d);

        assertEquals(200, result.getCode());
        verify(documentService).createDocument(d);
    }

    @Test
    @DisplayName("更新文档：委托 updateDocument（版本快照由其内部生成）")
    void update_delegatesToUpdate() {
        KbDocument newDoc = doc(10L, "新标题", 3L, "新正文");
        when(documentService.updateDocument(newDoc)).thenReturn(true);

        Result<Void> result = controller.update(newDoc);

        assertEquals(200, result.getCode());
        verify(documentService).updateDocument(newDoc);
    }

    @Test
    @DisplayName("更新不存在的文档：updateDocument 抛业务异常向上传播")
    void update_missingDocument_rejected() {
        KbDocument newDoc = doc(99L, "新标题", 3L, "新正文");
        when(documentService.updateDocument(newDoc))
                .thenThrow(new BusinessException("文档不存在"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.update(newDoc));

        assertTrue(ex.getMessage().contains("不存在"));
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