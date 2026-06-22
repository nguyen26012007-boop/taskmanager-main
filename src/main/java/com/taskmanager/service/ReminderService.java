package com.taskmanager.service;

import com.taskmanager.controller.ReminderController;
import com.taskmanager.model.Reminder;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service kiểm tra và gửi nhắc nhở theo lịch chạy phía Client.
 * Gọi ReminderController để kiểm tra thay vì kết nối DB trực tiếp.
 */
public class ReminderService {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ReminderService");
        t.setDaemon(true);
        return t;
    });

    private final ReminderController reminderController = new ReminderController();

    private Consumer<Reminder> notificationCallback;
    private int unreadCount = 0;
    private Consumer<Integer> badgeCallback;

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkReminders, 5, 30, TimeUnit.SECONDS);
        System.out.println("Reminder service đã khởi động");
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void checkReminders() {
        try {
            // Đảm bảo chỉ kiểm tra nếu user đã đăng nhập (đã thiết lập session)
            if (com.taskmanager.util.SessionContext.hasCurrentUser()) {
                List<Reminder> pending = reminderController.checkPendingReminders();
                for (Reminder reminder : pending) {
                    unreadCount++;
                    Platform.runLater(() -> {
                        if (notificationCallback != null) {
                            notificationCallback.accept(reminder);
                        }
                        if (badgeCallback != null) {
                            badgeCallback.accept(unreadCount);
                        }
                        showSystemNotification(reminder);
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi kiểm tra reminder từ Client: " + e.getMessage());
        }
    }

    private void showSystemNotification(Reminder reminder) {
        try {
            if (java.awt.SystemTray.isSupported()) {
                java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
                if (tray.getTrayIcons().length > 0) {
                    tray.getTrayIcons()[0].displayMessage(
                        "Task Manager - Nhắc nhở",
                        reminder.getTaskTitle(),
                        java.awt.TrayIcon.MessageType.INFO
                    );
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void setNotificationCallback(Consumer<Reminder> callback) {
        this.notificationCallback = callback;
    }

    public void setBadgeCallback(Consumer<Integer> callback) {
        this.badgeCallback = callback;
    }

    public void clearBadge() {
        unreadCount = 0;
        if (badgeCallback != null) {
            Platform.runLater(() -> badgeCallback.accept(0));
        }
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
