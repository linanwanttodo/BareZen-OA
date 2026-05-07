package com.linan.kb.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识文档删除所有权双轨校验 单测
 *
 * <p>覆盖 T13：作者本人 OR 持 kb:document:delete 权限的管理员均可删除；删除的双轨校验已下沉
 * 至 {@link IKbDocumentService#deleteDocument}，Controller 只做当前用户解析与 Result 委托，
 * 业务失败以 {@link BusinessException} 向上传播。方法级 @RequirePermission 已移除。</p>
 */
@ExtendWith(MockitoExtension.class)
class KbDocumentControllerDeleteTest {

    @Mock
    private IKbDocumentService documentService;

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
        when(documentService.deleteDocument(1L, 1L)).thenReturn(true);

        Result<Void> result = controller.delete(1L);

        assertEquals(200, result.getCode());
        verify(documentService).deleteDocument(1L, 1L);
    }

    @Test
    @DisplayName("非作者且无权限：抛 BusinessException 拒绝")
    void delete_notOwnerNoPermission_rejected() throws Exception {
        setCurrentUser(2L);
        when(documentService.deleteDocument(2L, 2L))
                .thenThrow(new BusinessException("无权限删除该文档"));

        BusinessException ex = assertThrows(
                BusinessException.class, () -> controller.delete(2L));

        assertTrue(ex.getMessage().contains("无权限"));
    }

    @Test
    @DisplayName("非作者但持删除权限（管理员旁路）：放行")
    void delete_notOwnerWithPermission_allowed() throws Exception {
        setCurrentUser(2L);
        when(documentService.deleteDocument(3L, 2L)).thenReturn(true);

        Result<Void> result = controller.delete(3L);

        assertEquals(200, result.getCode());
        verify(documentService).deleteDocument(3L, 2L);
    }

    @Test
    @DisplayName("文档不存在：抛 BusinessException")
    void delete_notFound_rejected() {
        when(documentService.deleteDocument(99L, 1L))
                .thenThrow(new BusinessException("文档不存在"));

        BusinessException ex = assertThrows(
                BusinessException.class, () -> controller.delete(99L));

        assertTrue(ex.getMessage().contains("不存在"));
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