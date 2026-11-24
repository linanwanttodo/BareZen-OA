package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.asset.dto.AssetBorrowRecordDTO;
import com.linan.asset.dto.AssetItemPublicDTO;
import com.linan.asset.entity.AssetBorrowRecord;
import com.linan.asset.entity.AssetCategory;
import com.linan.asset.entity.AssetItem;
import com.linan.asset.service.IAssetBorrowRecordService;
import com.linan.asset.service.IAssetCategoryService;
import com.linan.asset.service.IAssetItemService;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.interceptor.AuthInterceptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppAssetController 用户端资产借用单元测试
 *
 * <p>验证用户端口径：未登录拒绝、可借列表强制只返回在库资产（status 服务端注入）、
 * 公开视图不泄露管理字段、借用人身份服务端强制注入（接口不接收身份参数）、
 * 并发借用异常透传、我的借用归属过滤、归还的归属与重复归还校验。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppAssetControllerTest {

    @Mock
    private IAssetItemService assetService;

    @Mock
    private IAssetCategoryService categoryService;

    @Mock
    private IAssetBorrowRecordService borrowRecordService;

    private AppAssetController controller;

    @BeforeAll
    static void initTableInfoCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, AssetItem.class);
        TableInfoHelper.initTableInfo(assistant, AssetBorrowRecord.class);
        TableInfoHelper.initTableInfo(assistant, AssetCategory.class);
    }

    @BeforeEach
    void setUp() {
        controller = new AppAssetController(assetService, categoryService, borrowRecordService);
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
    @DisplayName("分类列表：复用管理端启用口径（status=1），未登录返回错误")
    void categories_enabledOnlyAndNotLoginRejected() throws Exception {
        // 未登录拒绝
        Result<?> rejected = controller.categories();
        assertEquals(500, rejected.getCode());
        assertEquals("请先登录", rejected.getMsg());
        verify(categoryService, never()).listCategories(any());

        setThreadLocal("currentUserIdHolder", 9L);
        AssetCategory category = new AssetCategory();
        category.setId(1L);
        category.setName("电脑办公");
        when(categoryService.listCategories(1)).thenReturn(List.of(category));

        Result<List<AssetCategory>> result = controller.categories();

        assertEquals(200, result.getCode());
        assertEquals("电脑办公", result.getData().get(0).getName());
        // 只查启用分类：必须传 status=1，不能拉全量
        verify(categoryService).listCategories(eq(1));
    }

    @Test
    @DisplayName("可借列表：强制只返回在库资产，接口不接收 status 参数")
    void items_forcedInStockStatus() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        Page<AssetItem> pageResult = new Page<>(1, 10);
        pageResult.setRecords(List.of(inStockItem()));
        pageResult.setTotal(1);
        when(assetService.page(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        var result = controller.items(1, 10, null, null);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getList().size());
        assertEquals("ThinkPad X1", result.getData().getList().get(0).getAssetName());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(assetService).page(any(IPage.class), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("status"), "查询条件必须包含状态过滤 status");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("0"), "必须过滤在库 status=0");
        assertTrue(sqlSegment.contains("asset_name") && sqlSegment.contains("ASC"), "必须按资产名称升序");
        // 接口不接收 status 参数：状态由服务端强制注入
        Method method = AppAssetController.class.getMethod("items", Integer.class, Integer.class, Long.class, String.class);
        assertTrue(java.util.Arrays.stream(method.getParameters())
                .map(java.lang.reflect.Parameter::getName).noneMatch("status"::equals),
                "可借列表接口不允许接收 status 参数");
    }

    @Test
    @DisplayName("可借列表：分类与名称关键字筛选生效")
    void items_filtersByCategoryAndName() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        when(assetService.page(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(new Page<>(1, 10));

        controller.items(1, 10, 2L, "笔记本");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(assetService).page(any(IPage.class), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("category_id"), "查询条件必须包含分类筛选 category_id");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(2L), "分类 ID 必须透传为查询条件");
        assertTrue(sqlSegment.contains("asset_name") && sqlSegment.contains("LIKE"), "名称关键字必须为 LIKE 查询");
        assertTrue(captor.getValue().getParamNameValuePairs().values().stream()
                        .anyMatch(v -> v instanceof String s && s.contains("笔记本")),
                "关键字必须透传为 LIKE 查询条件");
    }

    @Test
    @DisplayName("可借列表：未登录返回错误、分页参数越界被钳制")
    void items_notLoginAndInvalidPage_rejected() throws Exception {
        Result<?> rejected = controller.items(1, 10, null, null);
        assertEquals(500, rejected.getCode());
        assertEquals("请先登录", rejected.getMsg());

        setThreadLocal("currentUserIdHolder", 9L);
        assertThrows(BusinessException.class, () -> controller.items(0, 10, null, null));
        assertThrows(BusinessException.class, () -> controller.items(1, 101, null, null));
        verify(assetService, never()).page(any(), any());
    }

    @Test
    @DisplayName("资产详情：在库可见，公开视图不含管理字段；非在库不可见")
    void itemDetail_publicViewAndVisibility() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        when(assetService.getById(1L)).thenReturn(inStockItem());

        Result<AssetItemPublicDTO> result = controller.itemDetail(1L);

        assertEquals(200, result.getCode());
        assertEquals("ThinkPad X1", result.getData().getAssetName());
        // 公开视图不泄露管理字段：价格/购置日期/使用部门/使用人/报废字段无对应访问器
        assertTrue(java.util.Arrays.stream(AssetItemPublicDTO.class.getMethods())
                        .map(Method::getName).noneMatch(n -> List.of(
                                "getPrice", "getPurchaseDate", "getDeptName",
                                "getHolderName", "getScrapDate", "getRemark").contains(n)),
                "公开视图不得包含管理字段访问器");

        // 非在库（使用中）对用户端不可见
        AssetItem inUse = inStockItem();
        inUse.setStatus("1");
        when(assetService.getById(2L)).thenReturn(inUse);
        Result<AssetItemPublicDTO> denied = controller.itemDetail(2L);
        assertEquals(500, denied.getCode());
        assertEquals("资产不存在或已不可借用", denied.getMsg());
    }

    @Test
    @DisplayName("资产详情：记录不存在与未登录返回错误")
    void itemDetail_missingAndNotLogin_rejected() throws Exception {
        Result<?> notLogin = controller.itemDetail(1L);
        assertEquals(500, notLogin.getCode());
        assertEquals("请先登录", notLogin.getMsg());
        verify(assetService, never()).getById(any());

        setThreadLocal("currentUserIdHolder", 9L);
        when(assetService.getById(404L)).thenReturn(null);
        Result<AssetItemPublicDTO> missing = controller.itemDetail(404L);
        assertEquals(500, missing.getCode());
        assertEquals("资产不存在或已不可借用", missing.getMsg());
    }

    @Test
    @DisplayName("申请借用：借用人身份服务端强制注入，复用同一借出 Service 方法")
    void borrow_forcedCurrentUserIdentity() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        setThreadLocal("currentUsernameHolder", "zhangsan");

        Result<Void> result = controller.borrow(10L, "2026-08-30", "项目使用");

        assertEquals(200, result.getCode());
        // 必须调用带 borrowerId 的用户端口径：userId/姓名均来自服务端登录态
        verify(assetService).borrowAsset(10L, 9L, "zhangsan", "2026-08-30", "项目使用");
        // 不得走管理端无归属口径（borrowerId=null 的 4 参重载）
        verify(assetService, never()).borrowAsset(anyLong(), any(), any(), any());
        // 接口不接收身份参数：borrowerId/borrowerName/userId 均不可由前端传入
        Method method = AppAssetController.class.getMethod("borrow", Long.class, String.class, String.class);
        assertTrue(java.util.Arrays.stream(method.getParameters())
                        .map(java.lang.reflect.Parameter::getName).noneMatch(n ->
                                n.equals("borrowerId") || n.equals("borrowerName") || n.equals("userId")),
                "借用接口不允许接收身份参数");
    }

    @Test
    @DisplayName("申请借用：未登录返回错误")
    void borrow_notLogin_rejected() throws Exception {
        Result<?> result = controller.borrow(10L, null, null);
        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
        verify(assetService, never()).borrowAsset(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("申请借用：并发重复借用时 Service 异常透传")
    void borrow_concurrentConflict_propagates() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        setThreadLocal("currentUsernameHolder", "zhangsan");
        doThrow(new BusinessException("该资产已被并发借用，请刷新后重试"))
                .when(assetService).borrowAsset(eq(10L), eq(9L), any(), any(), any());

        assertThrows(BusinessException.class, () -> controller.borrow(10L, null, null));
    }

    @Test
    @DisplayName("我的借用：强制按当前登录用户过滤，记录附资产名称")
    void myBorrows_forcedCurrentUserAndEnriched() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        Page<AssetBorrowRecord> pageResult = new Page<>(1, 10);
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setId(100L);
        record.setAssetId(10L);
        record.setBorrowerId(9L);
        record.setBorrowerName("zhangsan");
        record.setStatus("0");
        pageResult.setRecords(List.of(record));
        pageResult.setTotal(1);
        when(borrowRecordService.page(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);
        when(assetService.listByIds(any())).thenReturn(List.of(inStockItem()));

        var result = controller.myBorrows(1, 10);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getList().size());
        AssetBorrowRecordDTO dto = result.getData().getList().get(0);
        assertEquals(10L, dto.getAssetId());
        assertEquals("ZC-2024-001", dto.getAssetNo(), "记录必须冗余资产编号供列表展示");
        assertEquals("ThinkPad X1", dto.getAssetName(), "记录必须冗余资产名称供列表展示");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(borrowRecordService).page(any(IPage.class), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("borrower_id"), "查询条件必须包含归属字段 borrower_id");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(9L),
                "borrowerId 必须取当前登录用户");
        // 接口不接收 userId：无法伪造他人借用记录
        Method method = AppAssetController.class.getMethod("myBorrows", Integer.class, Integer.class);
        assertTrue(java.util.Arrays.stream(method.getParameters())
                .map(java.lang.reflect.Parameter::getName).noneMatch("userId"::equals),
                "我的借用接口不允许接收 userId 参数");
    }

    @Test
    @DisplayName("我的借用：未登录返回错误、分页参数越界被钳制")
    void myBorrows_notLoginAndInvalidPage_rejected() throws Exception {
        Result<?> rejected = controller.myBorrows(1, 10);
        assertEquals(500, rejected.getCode());
        assertEquals("请先登录", rejected.getMsg());

        setThreadLocal("currentUserIdHolder", 9L);
        assertThrows(BusinessException.class, () -> controller.myBorrows(0, 10));
        assertThrows(BusinessException.class, () -> controller.myBorrows(1, 101));
        verify(borrowRecordService, never()).page(any(), any());
    }

    @Test
    @DisplayName("归还：仅本人可归还，复用管理端归还 Service（按资产ID）")
    void returnBorrow_ownerOnlyAndDelegates() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        AssetBorrowRecord mine = borrowRecord(100L, 9L, "0");
        when(borrowRecordService.getById(100L)).thenReturn(mine);

        Result<Void> result = controller.returnBorrow(100L);

        assertEquals(200, result.getCode());
        // 归还动作必须复用管理端口径：按资产ID原子回库并同步借用记录
        verify(assetService).returnAsset(mine.getAssetId());
    }

    @Test
    @DisplayName("归还：非本人与管理端代借记录（无 borrowerId）一律拒绝")
    void returnBorrow_ownershipEnforced() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        AssetBorrowRecord others = borrowRecord(101L, 2L, "0");
        when(borrowRecordService.getById(101L)).thenReturn(others);
        Result<Void> othersResult = controller.returnBorrow(101L);
        assertEquals(500, othersResult.getCode());
        assertEquals("无权归还该借用记录", othersResult.getMsg());

        AssetBorrowRecord adminCreated = borrowRecord(102L, null, "0");
        when(borrowRecordService.getById(102L)).thenReturn(adminCreated);
        Result<Void> adminResult = controller.returnBorrow(102L);
        assertEquals(500, adminResult.getCode());
        assertEquals("无权归还该借用记录", adminResult.getMsg());

        verify(assetService, never()).returnAsset(any());
    }

    @Test
    @DisplayName("归还：已归还记录不可重复归还，记录不存在与未登录返回错误")
    void returnBorrow_alreadyReturnedAndMissing_rejected() throws Exception {
        Result<?> notLogin = controller.returnBorrow(100L);
        assertEquals(500, notLogin.getCode());
        assertEquals("请先登录", notLogin.getMsg());
        verify(borrowRecordService, never()).getById(any());

        setThreadLocal("currentUserIdHolder", 9L);
        AssetBorrowRecord returned = borrowRecord(103L, 9L, "1");
        when(borrowRecordService.getById(103L)).thenReturn(returned);
        Result<Void> duplicated = controller.returnBorrow(103L);
        assertEquals(500, duplicated.getCode());
        assertEquals("该借用记录已归还，请勿重复操作", duplicated.getMsg());

        when(borrowRecordService.getById(404L)).thenReturn(null);
        Result<Void> missing = controller.returnBorrow(404L);
        assertEquals(500, missing.getCode());
        assertEquals("借用记录不存在", missing.getMsg());

        verify(assetService, never()).returnAsset(any());
    }

    private AssetItem inStockItem() {
        AssetItem item = new AssetItem();
        item.setId(10L);
        item.setAssetNo("ZC-2024-001");
        item.setAssetName("ThinkPad X1");
        item.setCategoryId(1L);
        item.setCategory("computer");
        item.setBrand("联想");
        item.setModel("X1 Carbon");
        item.setLocation("A栋3层IT库房");
        item.setStatus("0");
        return item;
    }

    private AssetBorrowRecord borrowRecord(Long id, Long borrowerId, String status) {
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setId(id);
        record.setAssetId(10L);
        record.setBorrowerId(borrowerId);
        record.setBorrowerName("zhangsan");
        record.setStatus(status);
        return record;
    }
}
