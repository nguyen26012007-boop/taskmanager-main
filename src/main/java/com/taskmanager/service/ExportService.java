package com.taskmanager.service;

import com.taskmanager.model.Task;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service xuất dữ liệu task ra các định dạng PDF, Excel, CSV
 */
public class ExportService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Xuất danh sách task ra file CSV (UTF-8 BOM - tương thích Excel)
     */
    public void exportCSV(List<Task> tasks, String filePath) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(filePath), java.nio.charset.StandardCharsets.UTF_8)) {
            // BOM cho Excel đọc được tiếng Việt
            writer.write('\uFEFF');
            // Header
            writer.write("ID,Tiêu đề,Mô tả,Thư mục,Ưu tiên,Trạng thái,Ngày bắt đầu,Ngày đến hạn,Ngày tạo\n");
            // Data
            for (Task task : tasks) {
                writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",%s,%s,%s,%s,%s\n",
                    task.getId(),
                    escapeCSV(task.getTitle()),
                    escapeCSV(task.getDescription()),
                    escapeCSV(task.getFolderName()),
                    task.getPriorityText(),
                    task.getStatusText(),
                    task.getStartDate() != null ? task.getStartDate().format(DF) : "",
                    task.getDueDate() != null ? task.getDueDate().format(DF) : "",
                    task.getCreatedAt() != null ? task.getCreatedAt().format(DTF) : ""
                ));
            }
        }
    }

    /**
     * Xuất danh sách task ra file Excel (.xlsx)
     */
    public void exportExcel(List<Task> tasks, String filePath) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // ===== Sheet 1: Danh sách task =====
            XSSFSheet sheet = workbook.createSheet("Danh sách Task");

            // Tạo styles
            XSSFCellStyle headerStyle = createHeaderStyle(workbook);
            XSSFCellStyle highPriorityStyle = createPriorityStyle(workbook, "E94560");
            XSSFCellStyle medPriorityStyle = createPriorityStyle(workbook, "FFB800");
            XSSFCellStyle lowPriorityStyle = createPriorityStyle(workbook, "00C896");
            XSSFCellStyle doneStyle = createDoneStyle(workbook);

            // Header row
            String[] headers = {"#", "Tiêu đề", "Thư mục", "Ưu tiên", "Trạng thái",
                                 "Ngày bắt đầu", "Ngày đến hạn", "Hoàn thành (%)"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                Row row = sheet.createRow(i + 1);

                // Chọn style theo ưu tiên
                XSSFCellStyle rowStyle = switch (task.getPriority()) {
                    case HIGH -> highPriorityStyle;
                    case MEDIUM -> medPriorityStyle;
                    case LOW -> lowPriorityStyle;
                };
                if (task.getStatus() == Task.Status.DONE) rowStyle = doneStyle;

                row.createCell(0).setCellValue(task.getId());
                row.createCell(1).setCellValue(task.getTitle());
                row.createCell(2).setCellValue(task.getFolderName() != null ? task.getFolderName() : "");
                row.createCell(3).setCellValue(task.getPriorityText());
                row.createCell(4).setCellValue(task.getStatusText());
                row.createCell(5).setCellValue(task.getStartDate() != null ? task.getStartDate().format(DF) : "");
                row.createCell(6).setCellValue(task.getDueDate() != null ? task.getDueDate().format(DF) : "");
                row.createCell(7).setCellValue(String.format("%.0f%%", task.getCompletionPercent()));

                for (int j = 0; j < 8; j++) {
                    if (row.getCell(j) != null) row.getCell(j).setCellStyle(rowStyle);
                }
            }

            // Auto-fit columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1024, 15000));
            }

            // Bật auto filter
            sheet.setAutoFilter(new CellRangeAddress(0, tasks.size(), 0, headers.length - 1));

            // ===== Sheet 2: Thống kê =====
            XSSFSheet statsSheet = workbook.createSheet("Thống kê");
            long total = tasks.size();
            long done = tasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();
            long inProgress = tasks.stream().filter(t -> t.getStatus() == Task.Status.IN_PROGRESS).count();
            long todo = tasks.stream().filter(t -> t.getStatus() == Task.Status.TODO).count();
            long overdue = tasks.stream().filter(Task::isOverdue).count();

            String[][] statsData = {
                {"Chỉ số", "Giá trị"},
                {"Tổng công việc", String.valueOf(total)},
                {"Hoàn thành", String.valueOf(done)},
                {"Đang thực hiện", String.valueOf(inProgress)},
                {"Chưa bắt đầu", String.valueOf(todo)},
                {"Quá hạn", String.valueOf(overdue)},
                {"Tỷ lệ hoàn thành", total > 0 ? String.format("%.1f%%", (double) done / total * 100) : "0%"}
            };

            for (int i = 0; i < statsData.length; i++) {
                Row row = statsSheet.createRow(i);
                for (int j = 0; j < statsData[i].length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(statsData[i][j]);
                    if (i == 0) cell.setCellStyle(headerStyle);
                }
            }
            statsSheet.autoSizeColumn(0);
            statsSheet.autoSizeColumn(1);

            // Ghi file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * Xuất danh sách task ra file PDF
     */
    public void exportPDF(List<Task> tasks, String filePath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float yPos = page.getMediaBox().getHeight() - margin;
                float pageWidth = page.getMediaBox().getWidth() - 2 * margin;

                // Vẽ header - nền màu tối
                cs.setNonStrokingColor(26f/255, 26f/255, 46f/255);
                cs.addRect(0, yPos - 10, page.getMediaBox().getWidth(), 60);
                cs.fill();

                // Tiêu đề
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 22);
                cs.setNonStrokingColor(234f/255, 69f/255, 96f/255);
                cs.newLineAtOffset(margin, yPos + 20);
                cs.showText("PERSONAL TASK MANAGER");
                cs.endText();

                // Subtitle
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.setNonStrokingColor(0.7f, 0.7f, 0.7f);
                cs.newLineAtOffset(margin, yPos + 5);
                cs.showText("Báo cáo xuất ngày " + LocalDateTime.now().format(DTF) + "  |  Tổng: " + tasks.size() + " task");
                cs.endText();

                yPos -= 25;

                // Đường kẻ
                cs.setStrokingColor(234f/255, 69f/255, 96f/255);
                cs.setLineWidth(1.5f);
                cs.moveTo(margin, yPos);
                cs.lineTo(page.getMediaBox().getWidth() - margin, yPos);
                cs.stroke();

                yPos -= 20;

                // Cột header bảng
                float[] colWidths = {30, 180, 60, 80, 80, 80};
                String[] colHeaders = {"#", "Tiêu đề", "Ưu tiên", "Trạng thái", "Đến hạn", "% HT"};

                // Vẽ header bảng
                cs.setNonStrokingColor(15f/255, 52f/255, 96f/255);
                cs.addRect(margin, yPos - 15, pageWidth, 18);
                cs.fill();

                float xPos = margin + 5;
                for (int i = 0; i < colHeaders.length; i++) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9);
                    cs.setNonStrokingColor(1f, 1f, 1f);
                    cs.newLineAtOffset(xPos, yPos - 11);
                    cs.showText(colHeaders[i]);
                    cs.endText();
                    xPos += colWidths[i];
                }

                yPos -= 20;

                // Vẽ từng task
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                boolean alternate = false;

                for (Task task : tasks) {
                    if (yPos < 60) {
                        // Thêm trang mới nếu hết chỗ
                        cs.close();
                        PDPage newPage = new PDPage(PDRectangle.A4);
                        doc.addPage(newPage);
                        // (simplified - trong production cần create new content stream)
                        break;
                    }

                    // Nền xen kẽ
                    if (alternate) {
                        cs.setNonStrokingColor(0.96f, 0.96f, 0.98f);
                        cs.addRect(margin, yPos - 13, pageWidth, 16);
                        cs.fill();
                    }
                    alternate = !alternate;

                    // Text màu theo ưu tiên
                    float[] color = switch (task.getPriority()) {
                        case HIGH -> new float[]{0.91f, 0.27f, 0.38f};
                        case MEDIUM -> new float[]{1f, 0.72f, 0f};
                        case LOW -> new float[]{0f, 0.78f, 0.59f};
                    };

                    // Vẽ indicator ưu tiên
                    cs.setNonStrokingColor(color[0], color[1], color[2]);
                    cs.addRect(margin, yPos - 11, 3, 12);
                    cs.fill();

                    // Text
                    cs.setNonStrokingColor(0.1f, 0.1f, 0.2f);
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                    cs.newLineAtOffset(margin + 8, yPos - 8);
                    cs.showText(String.valueOf(task.getId()));
                    cs.newLineAtOffset(30, 0);
                    String title = task.getTitle().length() > 28 ? task.getTitle().substring(0, 28) + "..." : task.getTitle();
                    cs.showText(title);
                    cs.newLineAtOffset(180, 0);
                    cs.showText(task.getPriorityText());
                    cs.newLineAtOffset(60, 0);
                    cs.showText(task.getStatusText());
                    cs.newLineAtOffset(80, 0);
                    cs.showText(task.getDueDate() != null ? task.getDueDate().format(DF) : "-");
                    cs.newLineAtOffset(80, 0);
                    cs.showText(String.format("%.0f%%", task.getCompletionPercent()));
                    cs.endText();

                    yPos -= 16;
                }

                // Footer
                cs.setNonStrokingColor(0.6f, 0.6f, 0.7f);
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 8);
                cs.newLineAtOffset(margin, 30);
                cs.showText("Personal Task Manager  •  Được tạo tự động");
                cs.endText();
            }

            doc.save(filePath);
        }
    }

    // ========== Helper methods ==========

    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace("\n", " ");
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{15, 52, 96}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private XSSFCellStyle createPriorityStyle(XSSFWorkbook wb, String hexColor) {
        XSSFCellStyle style = wb.createCellStyle();
        byte r = (byte) Integer.parseInt(hexColor.substring(0, 2), 16);
        byte g = (byte) Integer.parseInt(hexColor.substring(2, 4), 16);
        byte b = (byte) Integer.parseInt(hexColor.substring(4, 6), 16);
        // Light tint
        byte lr = (byte) Math.min(255, (r & 0xFF) + 180);
        byte lg = (byte) Math.min(255, (g & 0xFF) + 180);
        byte lb = (byte) Math.min(255, (b & 0xFF) + 180);
        style.setFillForegroundColor(new XSSFColor(new byte[]{lr, lg, lb}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private XSSFCellStyle createDoneStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setStrikeout(true);
        font.setColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)240, (byte)240, (byte)240}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
