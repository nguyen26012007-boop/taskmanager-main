package com.taskmanager.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Tiện ích xử lý ngày tháng
 */
public class DateUtil {

    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DISPLAY_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter DB_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd/MM");

    /**
     * Chuyển LocalDate sang chuỗi hiển thị
     */
    public static String format(LocalDate date) {
        return date != null ? date.format(DISPLAY_DATE) : "";
    }

    /**
     * Tính số ngày còn lại đến deadline
     */
    public static long daysUntilDue(LocalDate dueDate) {
        if (dueDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    /**
     * Mô tả thời gian đến hạn theo ngôn ngữ tự nhiên
     */
    public static String humanizeDueDate(LocalDate dueDate) {
        if (dueDate == null) return "Không có hạn";
        long days = daysUntilDue(dueDate);
        if (days < 0) return "Quá hạn " + Math.abs(days) + " ngày";
        if (days == 0) return "Hôm nay";
        if (days == 1) return "Ngày mai";
        if (days <= 7) return "Còn " + days + " ngày";
        return format(dueDate);
    }
}
