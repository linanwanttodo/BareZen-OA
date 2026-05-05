package com.linan.kb.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.security.PermissionService;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.kb.entity.KbDocument;
import com.linan.kb.service.IKbDocumentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识文档删除所有权双轨校验 单测
 *
 * <p>覆盖 T13：作者本人 OR 持 kb:document:delete 权限的管理员均可删除；
 * 方法级 @RequirePermission 已移除（否则作者本人被切面拦截），@OperLog 保留。</p>
 */
@ExtendWith(MockitoExtension.class)
class KbDocumentControllerDeleteTest {

    @Mock
    private IKbDocumentService documentService;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private KbDocumentController controller;

    @BeforeEach
    void setUp() throws Exception {
        setCurrentUser(1L);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setCurrentUser(Long userId) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField("currentUserIdHolder");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(userId);
    }

    @Test
    @DisplayName("作者本人删除自己的文档放行")
    void delete_owner_allowed() {
        KbDocument doc = new KbDocument();
        doc.setId(1L);
        doc.setAuthorId(1L);
        when(documentService.getById(1L)).thenReturn(doc);
        when(documentService.removeById(1L)).thenReturn(true);

        Result<Void> result = controller.delete(1L);

        assertEquals(200, result.getCode());
        verify(documentService).removeById(1L);
    }

    @Test
    @DisplayName("非作者且无权限：拒绝且不触库")
    void delete_notOwnerNoPermission_rejected() throws Exception {
        setCurrentUser(2L);
        KbDocument doc = new KbDocument();
        doc.setId(2L);
        doc.setAuthorId(1L);
        when(documentService.getById(2L)).thenReturn(doc);
        when(permissionService.hasPermission(2L, "kb:document:delete")).thenReturn(false);

        Result<Void> result = controller.delete(2L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("无权限"));
        verify(documentService, never()).removeById(2L);
    }

    @Test
    @DisplayName("非作者但持删除权限（管理员旁路）：放行")
    void delete_notOwnerWithPermission_allowed() throws Exception {
        setCurrentUser(2L);
        KbDocument doc = new KbDocument();
        doc.setId(3L);
        doc.setAuthorId(1L);
        when(documentService.getById(3L)).thenReturn(doc);
        when(permissionService.hasPermission(2L, "kb:document:delete")).thenReturn(true);
        when(documentService.removeById(3L)).thenReturn(true);

        Result<Void> result = controller.delete(3L);

        assertEquals(200, result.getCode());
        verify(documentService).removeById(3L);
    }

    @Test
    @DisplayName("文档不存在返回错误")
    void delete_notFound_rejected() {
        when(documentService.getById(99L)).thenReturn(null);

        Result<Void> result = controller.delete(99L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不存在"));
        verify(documentService, never()).removeById(99L);
    }

    @Test
    @DisplayName("方法级 @RequirePermission 已移除（防作者本人被切面拦截），@OperLog 保留")
    void delete_annotationContract() throws Exception {
        Method method = KbDocumentController.class.getMethod("delete", Long.class);
        assertNull(method.getAnnotation(RequirePermission.class),
                "delete 必须移除方法级 @RequirePermission，否则作者本人被切面先行拦截");
        assertNotNull(method.getAnnotation(OperLog.class), "delete 必须保留 @OperLog");
    }
}