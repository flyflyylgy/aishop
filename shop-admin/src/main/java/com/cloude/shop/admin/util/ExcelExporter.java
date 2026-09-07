package com.cloude.shop.admin.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 导出工具（Apache POI XSSF）
 * 同步导出，数据量在万级以内，运营/财务报表场景足够。
 */
public final class ExcelExporter {

    private ExcelExporter() {
    }

    /**
     * 写出单个 sheet 的 xlsx 到 HTTP 响应（浏览器下载）
     *
     * @param sheetName sheet 名
     * @param headers   表头
     * @param rows      数据行（与表头顺序对应，null 单元格写空串）
     * @param filename  下载文件名（不含扩展名）
     */
    public static void write(HttpServletResponse response, String sheetName,
                             String[] headers, List<Object[]> rows, String filename) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 表头
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }
            // 数据
            int r = 1;
            for (Object[] row : rows) {
                Row dataRow = sheet.createRow(r++);
                for (int c = 0; c < headers.length; c++) {
                    Cell cell = dataRow.createCell(c);
                    Object v = c < row.length ? row[c] : null;
                    setCell(cell, v);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 中文列宽保底
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 14 * 256));
            }

            String encoded = URLEncoder.encode(filename + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    private static void setCell(Cell cell, Object v) {
        if (v == null) {
            cell.setCellValue("");
        } else if (v instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (v instanceof Boolean b) {
            cell.setCellValue(b);
        } else {
            cell.setCellValue(String.valueOf(v));
        }
    }
}
