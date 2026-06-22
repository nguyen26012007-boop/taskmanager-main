package com.taskmanager.server;

import com.taskmanager.util.DBConnection;

/**
 * Entry point khởi chạy TCP Socket Server cho Task Manager.
 */
public class ServerMain {

    public static void main(String[] args) {
        System.out.println("=== Khởi động Task Manager Server ===");
        try {
            // Khởi tạo Database
            DBConnection.initialize();

            // Khởi chạy Server
            TaskManagerServer server = new TaskManagerServer(9999);
            server.start();

            // Thêm Shutdown Hook để dọn dẹp khi tắt server
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Đang tắt Server...");
                server.stop();
                DBConnection.close();
                System.out.println("Server đã dừng.");
            }));

        } catch (Exception e) {
            System.err.println("Lỗi khởi chạy Server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
