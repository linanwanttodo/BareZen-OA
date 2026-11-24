package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.PermissionService;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.kb.entity.KbCategory;
import com.linan.kb.entity.KbDocument;
import com.linan.kb.service.IKbCategoryService;
import com.linan.kb.service.IKbDocumentService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppKbController 查询接口单元测试
 *
 * <p>验证用户端知识库只读口径：未登录拒绝、列表强制只返回已发布文档
 * （status 服务端注入、接口不接收 status 参数）、分类/关键字筛选、
 * 详情状态可见性校验（草稿非管理员不可见且不计浏览量）。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppKbControllerTest {

    @Mock
    private IKbDocumentService documentService;

    @Mock
    private IKbCategoryService categoryService;

    @Mock
    private PermissionService permissionService;

    private AppKbController controller;

    @BeforeAll
    static void initTableInfoCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, KbDocument.class);
        TableInfoHelper.initTableInfo(assistant, KbCategory.class);
    }

    @BeforeEach
    void setUp() {
        controller = new AppKbController(documentService, categoryService, permissionService);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    /** 反射注入当前登录用户（AuthInterceptor 的 ThreadLocal 无公开 setter） */
    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("分类列表：仅返回启用状态，按排序号升序")
    void categories_enabledAndSorted() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        KbCategory category = new KbCategory();
        category.setId(1L);
        category.setName("制度手册");
        when(categoryService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(category));

        Result<List<KbCategory>> result = controller.categories();

        assertEquals(200, result.getCode());
        assertEquals("制度手册", result.getData().get(0).getName());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(categoryService).list(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("status"), "查询条件必须包含启用状态 status");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(1), "必须过滤 status=1 启用");
        assertTrue(sqlSegment.contains("sort") && sqlSegment.contains("ASC"), "必须按排序号升序");
    }

    @Test
    @DisplayName("文档列表：强制只返回已发布文档，接口不接收 status 参数")
    void documents_forcedPublishedStatus() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        Page<KbDocument> pageResult = new Page<>(1, 10);
        pageResult.setRecords(List.of(publishedDoc()));
        pageResult.setTotal(1);
        when(documentService.page(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        var result = controller.documents(1, 10, null, null);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getList().size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(documentService).page(any(IPage.class), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("status"), "查询条件必须包含状态过滤 status");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("1"), "必须过滤已发布 status=1");
        assertTrue(sqlSegment.contains("create_time") && sqlSegment.contains("DESC"), "必须按创建时间倒序");
        // 接口不接收 status 参数：状态由服务端强制注入
        Method method = AppKbController.class.getMethod("documents", Integer.class, Integer.class, Long.class, String.class);
        assertTrue(java.util.Arrays.stream(method.getParameters())
                .map(java.lang.reflect.Parameter::getName).noneMatch("status"::equals),
                "文档列表接口不允许接收 status 参数");
    }

    @Test
    @DisplayName("文档列表：分类与标题关键字筛选生效")
    void documents_filtersByCategoryAndTitle() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        when(documentService.page(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(new Page<>(1, 10));

        controller.documents(1, 10, 2L, "报销");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(documentService).page(any(IPage.class), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("category_id"), "查询条件必须包含分类筛选 category_id");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(2L), "分类 ID 必须透传为查询条件");
        assertTrue(sqlSegment.contains("title") && sqlSegment.contains("LIKE"), "标题关键字必须为 LIKE 查询");
        assertTrue(captor.getValue().getParamNameValuePairs().values().stream()
                        .anyMatch(v -> v instanceof String s && s.contains("报销")),
                "关键字必须透传为 LIKE 查询条件");
    }

    @Test
    @DisplayName("文档列表：未登录返回错误")
    void documents_notLogin_rejected() {
        Result<?> result = controller.documents(1, 10, null, null);

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
        verify(documentService, never()).page(any(), any());
    }

    @Test
    @DisplayName("文档列表：分页参数越界被钳制")
    void documents_invalidPage_rejected() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);

        assertThrows(BusinessException.class, () -> controller.documents(0, 10, null, null));
        assertThrows(BusinessException.class, () -> controller.documents(1, 101, null, null));
        verify(documentService, never()).page(any(), any());
    }

    @Test
    @DisplayName("分类列表：未登录返回错误")
    void categories_notLogin_rejected() {
        Result<?> result = controller.categories();

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
        verify(categoryService, never()).list(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("文档详情：已发布文档普通员工可见并自增浏览量")
    void detail_publishedVisibleAndCountsView() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        when(documentService.getById(1L)).thenReturn(publishedDoc());
        when(permissionService.hasPermission(9L, "kb:document:list")).thenReturn(false);
        when(documentService.viewDocument(1L)).thenReturn(publishedDoc());

        Result<KbDocument> result = controller.detail(1L);

        assertEquals(200, result.getCode());
        assertEquals("员工手册", result.getData().getTitle());
        verify(documentService).viewDocument(1L);
    }

    @Test
    @DisplayName("文档详情：草稿对普通员工不可见且不计浏览量，管理员可见")
    void detail_draftVisibilityByRole() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        KbDocument draft = publishedDoc();
        draft.setStatus("0");
        when(documentService.getById(2L)).thenReturn(draft);
        when(permissionService.hasPermission(9L, "kb:document:list")).thenReturn(false);

        Result<KbDocument> denied = controller.detail(2L);

        assertEquals(500, denied.getCode());
        assertEquals("文档未发布或已下线", denied.getMsg());
        verify(documentService, never()).viewDocument(any());

        // 管理员（持 kb:document:list 权限）可查看草稿
        setThreadLocal("currentUserIdHolder", 1L);
        when(permissionService.hasPermission(1L, "kb:document:list")).thenReturn(true);
        when(documentService.viewDocument(2L)).thenReturn(draft);
        Result<KbDocument> allowed = controller.detail(2L);
        assertEquals(200, allowed.getCode());
        verify(documentService).viewDocument(2L);
    }

    @Test
    @DisplayName("文档详情：记录不存在返回错误")
    void detail_missing_rejected() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        when(documentService.getById(404L)).thenReturn(null);

        Result<KbDocument> result = controller.detail(404L);

        assertEquals(500, result.getCode());
        assertEquals("文档不存在", result.getMsg());
        verify(documentService, never()).viewDocument(anyLong());
        verify(permissionService, never()).hasPermission(anyLong(), anyString());
    }

    @Test
    @DisplayName("文档详情：未登录返回错误")
    void detail_notLogin_rejected() {
        Result<?> result = controller.detail(1L);

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
        verify(documentService, never()).getById(any());
        verify(documentService, never()).viewDocument(any());
    }

    @Test
    @DisplayName("全部接口均为只读 GET，不接受任何写路径与越权参数")
    void endpoints_readOnlyAndNoPrivilegeParams() throws Exception {
        Method categories = AppKbController.class.getMethod("categories");
        assertEquals(0, categories.getParameterCount(), "分类列表接口不接受任何参数");
        Method documents = AppKbController.class.getMethod("documents", Integer.class, Integer.class, Long.class, String.class);
        assertEquals(4, documents.getParameterCount(), "文档列表只允许分页/分类/标题参数");
        Method detail = AppKbController.class.getMethod("detail", Long.class);
        assertEquals(1, detail.getParameterCount(), "详情接口只允许记录 id");
    }

    private KbDocument publishedDoc() {
        KbDocument doc = new KbDocument();
        doc.setId(1L);
        doc.setTitle("员工手册");
        doc.setCategoryId(2L);
        doc.setStatus("1");
        doc.setViewCount(10);
        return doc;
    }
}
