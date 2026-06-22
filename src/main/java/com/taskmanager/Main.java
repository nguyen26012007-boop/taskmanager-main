package com.taskmanager;

import com.taskmanager.network.NetworkService;
import com.taskmanager.service.ReminderService;
import com.taskmanager.util.SessionContext;
import com.taskmanager.view.AuthView;
import com.taskmanager.view.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Entry point của ứng dụng Personal Task Manager (Client Mode)
 * Kết nối server qua TCP Socket, khởi tạo reminder service và giao diện chính.
 */
public class Main extends Application {

    private ReminderService reminderService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Kết nối đến Server
        try {
            NetworkService.getInstance().connect("localhost", 9999);
        } catch (IOException e) {
            System.err.println("Không thể kết nối đến server: " + e.getMessage());
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Lỗi kết nối");
            alert.setHeaderText("Không tìm thấy Server!");
            alert.setContentText("Vui lòng khởi động Server trước khi chạy ứng dụng Client.");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        // Khởi động reminder service
        reminderService = new ReminderService();
        reminderService.start();

        // Hiển thị cửa sổ chính
        final AuthView[] authViewHolder = new AuthView[1];
        AuthView authView = new AuthView(primaryStage, user -> {
            SessionContext.setCurrentUser(user);
            MainWindow mainWindow = new MainWindow(
                primaryStage,
                reminderService,
                user,
                () -> {
                    SessionContext.clear();
                    Platform.runLater(authViewHolder[0]::show);
                }
            );
            mainWindow.show();
        });
        authViewHolder[0] = authView;
        authView.show();
    }

    @Override
    public void stop() throws Exception {
        if (reminderService != null) {
            reminderService.stop();
        }
        NetworkService.getInstance().disconnect();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
