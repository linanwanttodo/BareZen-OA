package com.linan.core.common.util;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 导入导出工具基类
 *
 * <p>提供简单的 CSV 导出功能，兼容 Excel 打开。
 * 后续可扩展为基于 EasyExcel 或 Apache POI 的完整实现。</p>
 *
 * <p>安全说明：单元格统一按 RFC 4180 转义（逗号/引号/换行），并对以
 * {@code = + - @} 等字符开头的单元格加单引号前缀，防止 CSV 公式注入
 * （OWASP CSV Injection）。</p>
 *
 * @author BareZen Team
 */
@Slf4j
public final class ExcelUtil {

    /** 公式注入敏感前缀：以这些字符开头的单元格会被转义为文本 */
    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    private ExcelUtil() {}

    /**
     * 导出 CSV（兼容 Excel 打开）
     *
     * @param response HTTP 响应
     * @param fileName 文件名（如 "用户数据.csv"）
     * @param headers  表头列表
     * @param rows     数据行列表
     */
    public static void exportCsv(HttpServletResponse response, String fileName,
                                  List<String> headers, List<List<String>> rows) {
        try {
            response.setContentType("text/csv;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + encodedName);

            // 写入 BOM 使 Excel 正确识别 UTF-8
            OutputStream os = response.getOutputStream();
            os.write(0xEF);
            os.write(0xBB);
            os.write(0xBF);

            // 写入表头
            if (headers != null && !headers.isEmpty()) {
                os.write(joinCsv(headers).getBytes(StandardCharsets.UTF_8));
                os.write('\n');
            }

            // 写入数据行
            if (rows != null) {
                for (List<String> row : rows) {
                    if (row != null) {
                        os.write(joinCsv(row).getBytes(StandardCharsets.UTF_8));
                        os.write('\n');
                    }
                }
            }

            os.flush();
        } catch (Exception e) {
            log.error("导出 CSV 失败: {}", e.getMessage());
        }
    }

    /** 拼接一行 CSV：逐单元格转义后以逗号连接 */
    private static String joinCsv(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsvCell(cells.get(i)));
        }
        return sb.toString();
    }

    /**
     * 转义单个 CSV 单元格（RFC 4180 + 公式注入防护）
     *
     * <p>字段含逗号/双引号/换行时以双引号包裹并双写内部引号；
     * 以公式敏感字符开头的字段先加 {@code '} 前缀避免被 Excel 当作公式执行。</p>
     */
    public static String escapeCsvCell(String cell) {
        if (cell == null || cell.isEmpty()) {
            return cell == null ? "" : cell;
        }
        String value = cell;
        char first = value.charAt(0);
        if (FORMULA_PREFIXES.indexOf(first) >= 0) {
            value = "'" + value;
        }
        boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}