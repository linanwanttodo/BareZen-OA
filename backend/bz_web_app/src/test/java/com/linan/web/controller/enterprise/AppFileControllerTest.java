package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysFile;
import com.linan.system.service.ISysFileService;
import com.linan.web.controller.dto.UserFileDTO;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
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
 * AppFileController 用户端「我的文件」单元测试
 *
 * <p>验证用户端口径：未登录拒绝、上传归属强制为当前登录用户（不接收身份参数）、
 * 列表强制按 create_by=当前用户过滤且不含文件夹、关键字为 LIKE 查询、
 * 下载/删除的越权拒绝（非本人文件一律 404/报错）、删除复用管理端 Service（含物理清理）。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppFileControllerTest {

    @Mock
    private ISysFileService fileService;

    private AppFileController controller;

    @BeforeAll
    static void initTableInfoCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysFile.class);
    }

    @BeforeEach
    void setUp() {
        controller = new AppFileController(fileService);
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
    @DisplayName("上传：未登录拒绝，且不触达文件服务")
    void upload_notLogin_rejected() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "报告.pdf", "application/pdf", "pdf".getBytes());

        Result<UserFileDTO> result = controller.upload(file);

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
        verify(fileService, never()).uploadFile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("上传：归属强制为当前登录用户，接口不接收身份参数")
    void upload_forcedCurrentUserOwnership() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        MultipartFile file = new MockMultipartFile("file", "报告.pdf", "application/pdf", "pdf".getBytes());
        SysFile saved = myFile(100L, 9L);
        when(fileService.uploadFile(any(), any(), any(), any())).thenReturn(saved);

        Result<UserFileDTO> result = controller.upload(file);

        assertEquals(200, result.getCode());
        assertEquals("报告.pdf", result.getData().getFileName());
        // 上传人必须为服务端注入的当前登录用户，scope 强制 private，文件夹固定根目录
        verify(fileService).uploadFile(any(), eq(0L), eq("private"), eq(9L));
        // 接口不接收身份参数：uploaderId/userId 均不可由前端传入
        Method method = AppFileController.class.getMethod("upload", MultipartFile.class);
        assertEquals(1, method.getParameterCount(), "上传接口只允许接收文件参数");
    }

    @Test
    @DisplayName("上传：超限/非法类型返回友好错误，不抛到全局异常")
    void upload_oversizeOrBadType_returnsFriendlyError() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        MultipartFile file = new MockMultipartFile("file", "病毒.exe", "application/octet-stream", "x".getBytes());
        when(fileService.uploadFile(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("不支持的文件类型: .exe"));

        Result<UserFileDTO> result = controller.upload(file);

        assertEquals(500, result.getCode());
        assertEquals("不支持的文件类型: .exe", result.getMsg());
    }

    @Test
    @DisplayName("上传：IO 异常返回通用友好提示")
    void upload_ioException_returnsGenericError() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        MultipartFile file = new MockMultipartFile("file", "报告.pdf", "application/pdf", "pdf".getBytes());
        when(fileService.uploadFile(any(), any(), any(), any()))
                .thenThrow(new java.io.IOException("disk full"));

        Result<UserFileDTO> result = controller.upload(file);

        assertEquals(500, result.getCode());
        assertEquals("文件上传失败，请稍后重试", result.getMsg());
    }

    @Test
    @DisplayName("列表：强制按当前登录用户过滤（create_by）且排除文件夹")
    void myFiles_forcedCurrentUserAndExcludeFolder() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        Page<SysFile> pageResult = new Page<>(1, 10);
        pageResult.setRecords(List.of(myFile(100L, 9L)));
        pageResult.setTotal(1);
        when(fileService.page(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        var result = controller.myFiles(1, 10, null);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getList().size());
        UserFileDTO dto = result.getData().getList().get(0);
        assertEquals(100L, dto.getId());
        assertEquals("报告.pdf", dto.getFileName());
        assertEquals(1024L, dto.getFileSize());
        assertEquals("document", dto.getFileType());
        assertEquals("local", dto.getStorageType());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(fileService).page(any(IPage.class), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("create_by"), "查询条件必须包含归属字段 create_by");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(9L),
                "归属必须是当前登录用户 ID");
        assertTrue(sqlSegment.contains("is_folder"), "查询条件必须排除文件夹");
        assertTrue(sqlSegment.contains("create_time") && sqlSegment.contains("DESC"), "必须按创建时间倒序");

        // 接口不接收 userId：无法伪造他人文件列表
        Method method = AppFileController.class.getMethod("myFiles", Integer.class, Integer.class, String.class);
        assertTrue(java.util.Arrays.stream(method.getParameters())
                        .map(java.lang.reflect.Parameter::getName).noneMatch("userId"::equals),
                "我的文件列表接口不允许接收 userId 参数");
    }

    @Test
    @DisplayName("列表：文件名关键字为 LIKE 查询")
    void myFiles_filtersByFileName() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        when(fileService.page(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(new Page<>(1, 10));

        controller.myFiles(1, 10, "季度");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(fileService).page(any(IPage.class), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("original_name") && sqlSegment.contains("LIKE"), "关键字必须为 LIKE 查询");
        assertTrue(captor.getValue().getParamNameValuePairs().values().stream()
                        .anyMatch(v -> v instanceof String s && s.contains("季度")),
                "关键字必须透传为 LIKE 查询条件");
    }

    @Test
    @DisplayName("列表：未登录返回错误、分页参数越界被钳制")
    void myFiles_notLoginAndInvalidPage_rejected() throws Exception {
        Result<?> rejected = controller.myFiles(1, 10, null);
        assertEquals(500, rejected.getCode());
        assertEquals("请先登录", rejected.getMsg());

        setThreadLocal("currentUserIdHolder", 9L);
        assertThrows(BusinessException.class, () -> controller.myFiles(0, 10, null));
        assertThrows(BusinessException.class, () -> controller.myFiles(1, 101, null));
        verify(fileService, never()).page(any(), any());
    }

    @Test
    @DisplayName("下载：仅本人文件可下载，复用管理端下载 Service")
    void download_ownerOnlyAndDelegates() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        SysFile mine = myFile(100L, 9L);
        when(fileService.getById(100L)).thenReturn(mine);
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);

        controller.download(100L, response);

        verify(fileService).downloadFile(eq(100L), eq(response));
        verify(response, never()).setStatus(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("下载：非本人文件与不存在文件一律 404（不可探测存在性）")
    void download_ownershipEnforced() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);

        // 他人文件
        when(fileService.getById(101L)).thenReturn(myFile(101L, 2L));
        controller.download(101L, response);
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);

        // 无归属记录（管理端代传）
        when(fileService.getById(102L)).thenReturn(myFile(102L, null));
        controller.download(102L, response);
        verify(response, org.mockito.Mockito.times(2)).setStatus(HttpServletResponse.SC_NOT_FOUND);

        // 不存在
        when(fileService.getById(404L)).thenReturn(null);
        controller.download(404L, response);
        verify(response, org.mockito.Mockito.times(3)).setStatus(HttpServletResponse.SC_NOT_FOUND);

        verify(fileService, never()).downloadFile(anyLong(), any());
    }

    @Test
    @DisplayName("下载：未登录返回 401")
    void download_notLogin_rejected() throws Exception {
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);

        controller.download(100L, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(fileService, never()).getById(any());
    }

    @Test
    @DisplayName("删除：仅本人可删除，复用管理端删除 Service（含物理文件清理）")
    void delete_ownerOnlyAndDelegates() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        when(fileService.getById(100L)).thenReturn(myFile(100L, 9L));
        when(fileService.deleteFile(100L)).thenReturn(true);

        Result<Void> result = controller.delete(100L);

        assertEquals(200, result.getCode());
        // 删除动作必须复用管理端口径：逻辑删除记录 + 事务提交后清理本地存储物理文件
        verify(fileService).deleteFile(100L);
    }

    @Test
    @DisplayName("删除：非本人文件一律拒绝，且不触达删除 Service")
    void delete_ownershipEnforced() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        when(fileService.getById(101L)).thenReturn(myFile(101L, 2L));

        Result<Void> result = controller.delete(101L);

        assertEquals(500, result.getCode());
        assertEquals("文件不存在或无权操作", result.getMsg());
        verify(fileService, never()).deleteFile(anyLong());
    }

    @Test
    @DisplayName("删除：未登录返回错误、记录不存在返回错误")
    void delete_notLoginAndMissing_rejected() throws Exception {
        Result<?> notLogin = controller.delete(100L);
        assertEquals(500, notLogin.getCode());
        assertEquals("请先登录", notLogin.getMsg());
        verify(fileService, never()).getById(any());

        setThreadLocal("currentUserIdHolder", 9L);
        when(fileService.getById(404L)).thenReturn(null);
        Result<Void> missing = controller.delete(404L);
        assertEquals(500, missing.getCode());
        assertEquals("文件不存在或无权操作", missing.getMsg());
        verify(fileService, never()).deleteFile(anyLong());
    }

    @Test
    @DisplayName("删除：Service 返回失败时透传错误")
    void delete_serviceFailure_returnsError() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        when(fileService.getById(100L)).thenReturn(myFile(100L, 9L));
        when(fileService.deleteFile(100L)).thenReturn(false);

        Result<Void> result = controller.delete(100L);

        assertEquals(500, result.getCode());
        assertEquals("删除失败，文件不存在", result.getMsg());
    }

    @Test
    @DisplayName("下载：Service 异常透传，不被吞掉")
    void download_serviceException_propagates() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        when(fileService.getById(100L)).thenReturn(myFile(100L, 9L));
        doThrow(new BusinessException("文件下载失败: 磁盘不可读"))
                .when(fileService).downloadFile(eq(100L), any());

        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        assertThrows(BusinessException.class, () -> controller.download(100L, response));
    }

    private SysFile myFile(Long id, Long createBy) {
        SysFile file = new SysFile();
        file.setId(id);
        file.setFileName("uuid.pdf");
        file.setOriginalName("报告.pdf");
        file.setFilePath("2026/08/05/uuid.pdf");
        file.setFileSize(1024L);
        file.setFileType("document");
        file.setStorageType("local");
        file.setIsFolder(0);
        file.setFolderId(0L);
        file.setScope("private");
        file.setCreateBy(createBy);
        file.setCreateTime(LocalDateTime.of(2026, 8, 5, 10, 12, 0));
        return file;
    }
}
