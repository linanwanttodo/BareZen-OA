package com.linan.core.common.util;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ExcelUtil CSV 导出单测：RFC 4180 转义 + 公式注入防护
 */
class ExcelUtilTest {

    @Test
    @DisplayName("普通字段原样输出，null 按空串处理")
    void escapeCsvCell_plain() {
        assertEquals("abc", ExcelUtil.escapeCsvCell("abc"));
        assertEquals("123", ExcelUtil.escapeCsvCell("123"));
        assertEquals("", ExcelUtil.escapeCsvCell(""));
        assertEquals("", ExcelUtil.escapeCsvCell(null));
    }

    @Test
    @DisplayName("含逗号/引号/换行的字段以双引号包裹并双写内部引号")
    void escapeCsvCell_quoted() {
        assertEquals("\"a,b\"", ExcelUtil.escapeCsvCell("a,b"));
        assertEquals("\"a\"\"b\"", ExcelUtil.escapeCsvCell("a\"b"));
        assertEquals("\"a\nb\"", ExcelUtil.escapeCsvCell("a\nb"));
    }

    @Test
    @DisplayName("以 = + - @ 开头的字段加单引号前缀，防公式注入")
    void escapeCsvCell_formulaInjectionPrevented() {
        assertEquals("'=SUM(A1)", ExcelUtil.escapeCsvCell("=SUM(A1)"));
        assertEquals("'+1", ExcelUtil.escapeCsvCell("+1"));
        assertEquals("'-2", ExcelUtil.escapeCsvCell("-2"));
        assertEquals("'@cmd", ExcelUtil.escapeCsvCell("@cmd"));
        // 前缀 `'` 后再按需加引号包裹
        assertEquals("\"'=a,b\"", ExcelUtil.escapeCsvCell("=a,b"));
    }

    @Test
    @DisplayName("exportCsv：BOM + 表头 + 转义后的数据行")
    void exportCsv_endToEnd() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new jakarta.servlet.ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
            }

            @Override
            public void write(int b) {
                out.write(b);
            }
        });

        ExcelUtil.exportCsv(response, "测试.csv",
                List.of("ID", "名称"),
                List.of(
                        List.of("1", "=SUM(A1)"),
                        List.of("2", "张,三")));

        String csv = out.toString(StandardCharsets.UTF_8);
        // BOM 头（解码后为单字符）
        assertEquals(0xEF, out.toByteArray()[0] & 0xFF);
        assertEquals(0xBB, out.toByteArray()[1] & 0xFF);
        assertEquals(0xBF, out.toByteArray()[2] & 0xFF);
        assertTrue(csv.startsWith("\uFEFF"));

        String[] lines = csv.substring(1).split("\n");
        assertEquals(3, lines.length);
        assertEquals("ID,名称", lines[0]);
        assertEquals("1,'=SUM(A1)", lines[1]);
        assertEquals("2,\"张,三\"", lines[2]);
        org.mockito.Mockito.verify(response).setContentType("text/csv;charset=UTF-8");
    }
}