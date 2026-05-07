package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.storage.FileStorage;
import com.linan.system.entity.SysFile;
import com.linan.system.mapper.SysFileMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件服务单元测试（存储抽象适配，3.2）
 *
 * <p>验证 SysFileServiceImpl 与 {@link FileStorage} 的交互：上传走
 * storage.store（存储类型落库）、下载走 storage.load、删除走 storage.delete，
 * 并保留原有后缀白名单/路径穿越防御。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysFileServiceImplTest {

    @Mock
    private FileStorage storage;

    @Mock
    private SysFileMapper baseMapper;

    /** 被测服务：手动构造（注入 storage），再反射注入 MyBatis-Plus baseMapper */
    private SysFileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        fileService = new SysFileServiceImpl(storage);
        ReflectionTestUtils.setField(fileService, "baseMapper", baseMapper);
    }

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysFile.class);
    }

    @Test
    @DisplayName("上传：写入 storage 且落库 storageType=local、filePath 为日期相对路径")
    void uploadFile_delegatesToStorage() throws IOException {
        when(storage.getType()).thenReturn("local");
        MockMultipartFile file = new MockMultipartFile("file", "报告.pdf", "application/pdf", "pdf-content".getBytes());
        when(baseMapper.insert(any(SysFile.class))).thenReturn(1);
        when(baseMapper.updateById(any(SysFile.class))).thenReturn(1);

        SysFile result = fileService.uploadFile(file, null, "admin");

        assertEquals("local", result.getStorageType());
        assertEquals("document", result.getFileType());
        assertTrue(result.getFilePath().matches("\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{32}\\.pdf"),
                "相对路径应为 yyyy/MM/dd/uuid.pdf 格式，实际: " + result.getFilePath());
        verify(storage).store(any(), eq(11L), eq(result.getFilePath()));
    }

    @Test
    @DisplayName("上传：非法后缀被拒绝且不触达存储")
    void uploadFile_unsupportedSuffix_rejected() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "病毒.exe", "application/octet-stream", "x".getBytes());

        assertThrows(IllegalArgumentException.class, () -> fileService.uploadFile(file, null, null));
        verify(storage, never()).store(any(), any(long.class), any());
    }

    @Test
    @DisplayName("安全：SVG 上传被拒绝（防 /profile 存储型 XSS）")
    void uploadFile_svg_rejectedForStoredXss() throws IOException {
        // SVG 可内联执行脚本，上传目录又经 /profile 静态映射直接输出，存在存储型 XSS 风险
        MockMultipartFile svg = new MockMultipartFile(
                "file", "danger.svg", "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>document.location='https://evil'</script></svg>".getBytes());

        assertThrows(IllegalArgumentException.class, () -> fileService.uploadFile(svg, null, null));
        verify(storage, never()).store(any(), any(long.class), any());
    }

    @Test
    @DisplayName("下载：从 storage 读取并写入响应流")
    void downloadFile_streamsFromStorage() throws IOException {
        SysFile sysFile = new SysFile();
        sysFile.setId(1L);
        sysFile.setFilePath("2026/08/05/uuid.pdf");
        sysFile.setOriginalName("报告.pdf");
        sysFile.setFileName("uuid.pdf");
        sysFile.setFileSize(11L);
        when(baseMapper.selectById(1L)).thenReturn(sysFile);
        when(storage.load("2026/08/05/uuid.pdf")).thenReturn(new ByteArrayInputStream("pdf-content".getBytes()));

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener listener) { }
            @Override public void write(int b) { body.write(b); }
        });

        fileService.downloadFile(1L, response);

        assertEquals("pdf-content", body.toString());
        verify(storage).load("2026/08/05/uuid.pdf");
    }

    @Test
    @DisplayName("下载：路径穿越 filePath 被拒绝且不触达存储")
    void downloadFile_traversalPath_rejected() throws IOException {
        SysFile sysFile = new SysFile();
        sysFile.setId(1L);
        sysFile.setFilePath("../etc/passwd");
        when(baseMapper.selectById(1L)).thenReturn(sysFile);
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThrows(RuntimeException.class, () -> fileService.downloadFile(1L, response));
        verify(storage, never()).load(any());
    }

    @Test
    @DisplayName("删除：物理文件走 storage.delete 并删记录")
    void deleteFile_deletesFromStorage() {
        SysFile sysFile = new SysFile();
        sysFile.setId(1L);
        sysFile.setFilePath("2026/08/05/uuid.jpg");
        sysFile.setIsFolder(0);
        when(baseMapper.selectById(1L)).thenReturn(sysFile);
        when(baseMapper.deleteById(1L)).thenReturn(1);

        boolean ok = fileService.deleteFile(1L);

        assertTrue(ok);
        verify(storage).delete("2026/08/05/uuid.jpg");
        verify(baseMapper).deleteById(1L);
    }
}