package com.linan.kb.service.impl;

import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.PermissionService;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.kb.entity.KbDocument;
import com.linan.kb.entity.KbDocumentVersion;
import com.linan.kb.mapper.KbDocumentMapper;
import com.linan.kb.service.IKbDocumentVersionService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KbDocumentServiceImpl 单元测试：版本快照/作者兜底/删除双轨/浏览量下沉后的行为
 */
@ExtendWith(MockitoExtension.class)
class KbDocumentServiceImplTest {

    @Mock
    private KbDocumentMapper mapper;
    @Mock
    private IKbDocumentVersionService versionService;
    @Mock
    private PermissionService permissionService;

    private KbDocumentServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new KbDocumentServiceImpl(versionService, permissionService);
        Field baseMapper = KbDocumentServiceImpl.class.getSuperclass().getDeclaredField("baseMapper");
        baseMapper.setAccessible(true);
        baseMapper.set(service, mapper);

        setCurrentUser(7L);
        lenient().when(versionService.list(any(Wrapper.class))).thenReturn(java.util.List.of());
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setCurrentUser(Long userId) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField("currentUserIdHolder");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.lang.ThreadLocal<Object> tl = (java.lang.ThreadLocal<Object>) field.get(null);
        tl.set(userId);
    }

    @Test
    @DisplayName("createDocument：作者兜底为当前用户并写首版快照")
    void createDocument_writesVersion() {
        KbDocument doc = new KbDocument();
        doc.setTitle("入职指南");
        doc.setContent("内容");
        when(mapper.insert(any(KbDocument.class))).thenAnswer(inv -> {
            KbDocument d = inv.getArgument(0);
            d.setId(100L);
            return 1;
        });

        boolean ok = service.createDocument(doc);

        org.junit.jupiter.api.Assertions.assertTrue(ok);
        assertEquals(7L, doc.getAuthorId());
        assertEquals("0", doc.getStatus());
        verify(versionService).save(any(KbDocumentVersion.class));
    }

    @Test
    @DisplayName("updateDocument：文档不存在时抛业务异常")
    void updateDocument_notFound() {
        KbDocument doc = new KbDocument();
        doc.setId(5L);
        when(mapper.selectById(5L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.updateDocument(doc));
        verify(mapper, never()).updateById(any(KbDocument.class));
    }

    @Test
    @DisplayName("deleteDocument：非作者且无权限时抛业务异常")
    void deleteDocument_forbidden() {
        KbDocument doc = new KbDocument();
        doc.setId(9L);
        doc.setAuthorId(99L);
        when(mapper.selectById(9L)).thenReturn(doc);
        when(permissionService.hasPermission(7L, "kb:document:delete")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deleteDocument(9L, 7L));
        assertEquals("无权限删除该文档", ex.getMessage());
        verify(mapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteDocument：作者本人可删除")
    void deleteDocument_owner() {
        KbDocument doc = new KbDocument();
        doc.setId(9L);
        doc.setAuthorId(7L);
        when(mapper.selectById(9L)).thenReturn(doc);
        when(mapper.deleteById(9L)).thenReturn(1);

        org.junit.jupiter.api.Assertions.assertTrue(service.deleteDocument(9L, 7L));
        verify(mapper).deleteById(9L);
    }

    @Test
    @DisplayName("viewDocument：文档不存在抛业务异常")
    void viewDocument_notFound() {
        when(mapper.selectById(11L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.viewDocument(11L));
    }
}