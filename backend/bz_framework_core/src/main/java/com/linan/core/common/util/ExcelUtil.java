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
 * <p>使用方式：</p>
 * <pre>{@code
 * // Controller 中导出
 * @GetMapping("/export")
 * public void export(HttpServletResponse response) {
 *     List<String> headers = List.of("ID", "名称", "状态");
 *     List<List<String>> rows = service.getExportData();
 *     ExcelUtil.exportCsv(response, "用户数据.csv", headers, rows);
 * }
 * }</pre>
 */
@Slf4j
public final class ExcelUtil {

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
                os.write(String.join(",", headers).getBytes(StandardCharsets.UTF_8));
                os.write('\n');
            }

            // 写入数据行
            if (rows != null) {
                for (List<String> row : rows) {
                    if (row != null) {
                        os.write(String.join(",", row).getBytes(StandardCharsets.UTF_8));
                        os.write('\n');
                    }
                }
            }

            os.flush();
        } catch (Exception e) {
            log.error("导出 CSV 失败: {}", e.getMessage());
        }
    }
}
