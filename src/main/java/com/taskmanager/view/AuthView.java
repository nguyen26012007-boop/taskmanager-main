package com.taskmanager.view;

import com.taskmanager.controller.UserController;
import com.taskmanager.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class AuthView {
    public interface AuthSuccessHandler {
        void onSuccess(User user) throws Exception;
    }

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_RECOVERY_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final Stage stage;
    private final AuthSuccessHandler successHandler;
    private final UserController userController = new UserController();

    private final Map<String, Integer> loginAttempts = new HashMap<>();
    private final Map<String, Instant> loginLockedUntil = new HashMap<>();
    private final Map<String, Integer> recoveryAttempts = new HashMap<>();
    private final Map<String, Instant> recoveryLockedUntil = new HashMap<>();

    private Scene scene;
    private StackPane root;
    private VBox card;
    private Label statusLabel;

    public AuthView(Stage stage, AuthSuccessHandler successHandler) {
        this.stage = stage;
        this.successHandler = successHandler;
    }

    public void show() {
        if (scene == null) {
            buildUI();
        }
        if (hasAccountsSafely()) {
            showLogin();
        } else {
            showRegister();
        }
        stage.setScene(scene);
        stage.setTitle("Task Manager - Đăng nhập");
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/images/logo.png");
            if (iconStream != null) {
                stage.getIcons().add(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stage.setMinWidth(880);
        stage.setMinHeight(620);
        stage.centerOnScreen();
        stage.show();
    }

    private void buildUI() {
        root = new StackPane();
        root.getStyleClass().add("auth-root");

        HBox shell = new HBox();
        shell.setMaxWidth(980);
        shell.setMaxHeight(620);
        shell.getStyleClass().add("auth-shell");

        VBox brandPane = buildBrandPane();
        card = new VBox(18);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(32));
        card.setPrefWidth(430);

        shell.getChildren().addAll(brandPane, card);
        root.getChildren().add(shell);

        scene = new Scene(root, 980, 640);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
    }

    private VBox buildBrandPane() {
        VBox brand = new VBox(18);
        brand.getStyleClass().add("auth-brand");
        brand.setPadding(new Insets(40));
        brand.setPrefWidth(520);
        brand.setAlignment(Pos.CENTER_LEFT);

        Label eyebrow = new Label("PERSONAL TASK MANAGER");
        eyebrow.getStyleClass().add("auth-eyebrow");

        Label title = new Label("Quản lý công việc gọn hơn, tập trung hơn.");
        title.getStyleClass().add("auth-title");
        title.setWrapText(true);

        Label subtitle = new Label("Đăng nhập để tiếp tục theo dõi công việc, lịch nhắc và tiến độ trong một nơi.");
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);

        VBox bullets = new VBox(12,
            buildFeature("Theo dõi task theo card, list và kanban"),
            buildFeature("Nhắc việc tự động và thống kê nhanh"),
            buildFeature("Khôi phục mật khẩu bằng mã PIN 6 số"),
            buildFeature("Xuất báo cáo CSV, Excel và PDF"));

        brand.getChildren().addAll(eyebrow, title, subtitle, bullets);
        return brand;
    }

    private HBox buildFeature(String text) {
        Label dot = new Label("•");
        dot.getStyleClass().add("auth-feature-dot");
        Label label = new Label(text);
        label.getStyleClass().add("auth-feature-text");
        HBox row = new HBox(10, dot, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void showLogin() {
        card.getChildren().setAll(buildHeader(
            "Đăng nhập",
            "Tiếp tục với tài khoản của bạn.",
            "Chưa có tài khoản?",
            "Tạo tài khoản",
            this::showRegister
        ));

        TextField usernameField = new TextField();
        usernameField.getStyleClass().add("form-field");
        usernameField.setPromptText("Tên đăng nhập");

        PasswordField passwordField = new PasswordField();
        passwordField.getStyleClass().add("form-field");
        passwordField.setPromptText("Mật khẩu");

        Hyperlink forgotLink = new Hyperlink("Quên mật khẩu?");
        forgotLink.getStyleClass().add("link-button");
        forgotLink.setOnAction(e -> showForgotPasswordDialog());

        Button loginButton = new Button("Đăng nhập");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> handleLogin(usernameField.getText(), passwordField.getText()));

        statusLabel = new Label();
        statusLabel.getStyleClass().add("auth-status");

        card.getChildren().addAll(
            labeledField("Tên đăng nhập", usernameField),
            labeledField("Mật khẩu", passwordField),
            forgotLink,
            loginButton,
            statusLabel
        );
    }

    private void showRegister() {
        card.getChildren().setAll(buildHeader(
            "Tạo tài khoản",
            "Thiết lập tài khoản cục bộ để vào ứng dụng.",
            "Đã có tài khoản?",
            "Đăng nhập",
            this::showLogin
        ));

        TextField nameField = new TextField();
        nameField.getStyleClass().add("form-field");
        nameField.setPromptText("Tên hiển thị");

        TextField usernameField = new TextField();
        usernameField.getStyleClass().add("form-field");
        usernameField.setPromptText("Tên đăng nhập");

        PasswordField passwordField = new PasswordField();
        passwordField.getStyleClass().add("form-field");
        passwordField.setPromptText("Mật khẩu");

        PasswordField confirmField = new PasswordField();
        confirmField.getStyleClass().add("form-field");
        confirmField.setPromptText("Nhập lại mật khẩu");

        PasswordField recoveryPinField = new PasswordField();
        recoveryPinField.getStyleClass().add("form-field");
        recoveryPinField.setPromptText("Mã PIN 6 số");

        PasswordField confirmPinField = new PasswordField();
        confirmPinField.getStyleClass().add("form-field");
        confirmPinField.setPromptText("Nhập lại mã PIN 6 số");

        Label pinNote = new Label("Mã PIN 6 số dùng để khôi phục mật khẩu khi bạn quên mật khẩu đăng nhập.");
        pinNote.getStyleClass().add("page-subtitle");
        pinNote.setWrapText(true);

        Button registerButton = new Button("Đăng ký");
        registerButton.getStyleClass().add("primary-button");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setOnAction(e -> handleRegister(
            nameField.getText(),
            usernameField.getText(),
            passwordField.getText(),
            confirmField.getText(),
            recoveryPinField.getText(),
            confirmPinField.getText()
        ));

        statusLabel = new Label();
        statusLabel.getStyleClass().add("auth-status");

        card.getChildren().addAll(
            labeledField("Tên hiển thị", nameField),
            labeledField("Tên đăng nhập", usernameField),
            labeledField("Mật khẩu", passwordField),
            labeledField("Xác nhận mật khẩu", confirmField),
            labeledField("Mã PIN khôi phục", recoveryPinField),
            labeledField("Xác nhận mã PIN", confirmPinField),
            pinNote,
            registerButton,
            statusLabel
        );
    }

    private VBox buildHeader(String titleText, String subtitleText, String switchText, String switchAction, Runnable action) {
        Label title = new Label(titleText);
        title.getStyleClass().add("page-title");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);

        Label prompt = new Label(switchText);
        prompt.getStyleClass().add("auth-switch-label");

        Button switchButton = new Button(switchAction);
        switchButton.getStyleClass().add("link-button");
        switchButton.setOnAction(e -> action.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox switchRow = new HBox(6, prompt, switchButton, spacer);
        switchRow.setAlignment(Pos.CENTER_LEFT);

        return new VBox(8, title, subtitle, switchRow);
    }

    private VBox labeledField(String labelText, javafx.scene.Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        return new VBox(6, label, field);
    }

    private void handleLogin(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            setStatus("Nhập đầy đủ tên đăng nhập và mật khẩu.", true);
            return;
        }

        String normalizedUsername = username.trim().toLowerCase();
        String lockMessage = getLockMessage(loginLockedUntil, normalizedUsername);
        if (lockMessage != null) {
            setStatus(lockMessage, true);
            return;
        }

        try {
            User user = userController.login(username.trim(), password);
            if (user == null) {
                registerFailedAttempt(loginAttempts, loginLockedUntil, normalizedUsername, MAX_LOGIN_ATTEMPTS);
                String remaining = getLockMessage(loginLockedUntil, normalizedUsername);
                setStatus(remaining != null ? remaining : "Sai tên đăng nhập hoặc mật khẩu.", true);
                return;
            }

            clearAttemptState(loginAttempts, loginLockedUntil, normalizedUsername);
            successHandler.onSuccess(user);
        } catch (Exception e) {
            setStatus("Không thể đăng nhập: " + e.getMessage(), true);
        }
    }

    private void handleRegister(String name, String username, String password, String confirmPassword, String recoveryPin, String confirmPin) {
        if (name == null || name.isBlank() || username == null || username.isBlank()) {
            setStatus("Tên hiển thị và tên đăng nhập là bắt buộc.", true);
            return;
        }
        if (password == null || password.length() < 6) {
            setStatus("Mật khẩu cần ít nhất 6 ký tự.", true);
            return;
        }
        if (!password.equals(confirmPassword)) {
            setStatus("Mật khẩu xác nhận không khớp.", true);
            return;
        }
        if (!isValidPin(recoveryPin)) {
            setStatus("Mã PIN khôi phục phải đúng 6 chữ số.", true);
            return;
        }
        if (!recoveryPin.equals(confirmPin)) {
            setStatus("Mã PIN xác nhận không khớp.", true);
            return;
        }

        try {
            User user = userController.register(name.trim(), username.trim(), password, recoveryPin);
            if (user == null) {
                setStatus("Không thể tạo tài khoản.", true);
                return;
            }
            successHandler.onSuccess(user);
        } catch (Exception e) {
            setStatus("Không thể đăng ký: " + e.getMessage(), true);
        }
    }

    private void showForgotPasswordDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Khôi phục mật khẩu");
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        TextField usernameField = new TextField();
        usernameField.getStyleClass().add("form-field");
        usernameField.setPromptText("Tên đăng nhập");

        PasswordField recoveryPinField = new PasswordField();
        recoveryPinField.getStyleClass().add("form-field");
        recoveryPinField.setPromptText("Mã PIN 6 số");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.getStyleClass().add("form-field");
        newPasswordField.setPromptText("Mật khẩu mới");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.getStyleClass().add("form-field");
        confirmPasswordField.setPromptText("Nhập lại mật khẩu mới");

        Label note = new Label("Nhập đúng tên đăng nhập và mã PIN 6 số đã đặt để tạo mật khẩu mới.");
        note.getStyleClass().add("page-subtitle");
        note.setWrapText(true);

        Label dialogStatus = new Label();
        dialogStatus.getStyleClass().add("auth-status");

        VBox content = new VBox(12,
            labeledField("Tên đăng nhập", usernameField),
            labeledField("Mã PIN khôi phục", recoveryPinField),
            labeledField("Mật khẩu mới", newPasswordField),
            labeledField("Xác nhận mật khẩu mới", confirmPasswordField),
            note,
            dialogStatus
        );
        content.setPadding(new Insets(18));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        confirmButton.setText("Đặt lại mật khẩu");
        confirmButton.getStyleClass().add("primary-button");
        confirmButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
            String recoveryPin = recoveryPinField.getText() != null ? recoveryPinField.getText().trim() : "";
            String newPassword = newPasswordField.getText() != null ? newPasswordField.getText() : "";
            String confirmPassword = confirmPasswordField.getText() != null ? confirmPasswordField.getText() : "";
            String normalizedUsername = username.toLowerCase();

            String lockMessage = getLockMessage(recoveryLockedUntil, normalizedUsername);
            if (lockMessage != null) {
                dialogStatus.getStyleClass().remove("auth-status-success");
                dialogStatus.getStyleClass().add("auth-status-error");
                dialogStatus.setText(lockMessage);
                event.consume();
                return;
            }

            if (username.isEmpty()) {
                dialogStatus.setText("Nhập tên đăng nhập cần khôi phục.");
                dialogStatus.getStyleClass().remove("auth-status-success");
                dialogStatus.getStyleClass().add("auth-status-error");
                event.consume();
                return;
            }
            if (!isValidPin(recoveryPin)) {
                dialogStatus.setText("Mã PIN phải đúng 6 chữ số.");
                dialogStatus.getStyleClass().remove("auth-status-success");
                dialogStatus.getStyleClass().add("auth-status-error");
                event.consume();
                return;
            }
            if (newPassword.length() < 6) {
                dialogStatus.setText("Mật khẩu mới cần ít nhất 6 ký tự.");
                dialogStatus.getStyleClass().remove("auth-status-success");
                dialogStatus.getStyleClass().add("auth-status-error");
                event.consume();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                dialogStatus.setText("Mật khẩu xác nhận không khớp.");
                dialogStatus.getStyleClass().remove("auth-status-success");
                dialogStatus.getStyleClass().add("auth-status-error");
                event.consume();
                return;
            }

            try {
                boolean reset = userController.resetPassword(username, recoveryPin, newPassword);
                if (!reset) {
                    registerFailedAttempt(recoveryAttempts, recoveryLockedUntil, normalizedUsername, MAX_RECOVERY_ATTEMPTS);
                    String remaining = getLockMessage(recoveryLockedUntil, normalizedUsername);
                    dialogStatus.setText(remaining != null ? remaining : "Sai tên đăng nhập hoặc mã PIN khôi phục.");
                    dialogStatus.getStyleClass().remove("auth-status-success");
                    dialogStatus.getStyleClass().add("auth-status-error");
                    event.consume();
                    return;
                }

                clearAttemptState(recoveryAttempts, recoveryLockedUntil, normalizedUsername);
                dialog.close();
                showLogin();
                setStatus("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập lại ngay.", false);
            } catch (Exception ex) {
                dialogStatus.setText("Không thể đặt lại mật khẩu: " + ex.getMessage());
                dialogStatus.getStyleClass().remove("auth-status-success");
                dialogStatus.getStyleClass().add("auth-status-error");
                event.consume();
            }
        });

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancelButton.getStyleClass().add("secondary-button");

        dialog.showAndWait();
    }

    private boolean isValidPin(String pin) {
        return pin != null && pin.matches("\\d{6}");
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("auth-status-error", "auth-status-success");
        statusLabel.getStyleClass().add(error ? "auth-status-error" : "auth-status-success");
    }

    private boolean hasAccountsSafely() {
        try {
            return !userController.getAllUsers().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void registerFailedAttempt(Map<String, Integer> attempts, Map<String, Instant> locks, String key, int maxAttempts) {
        Instant now = Instant.now();
        Instant lockedUntil = locks.get(key);
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            return;
        }

        int nextCount = attempts.getOrDefault(key, 0) + 1;
        attempts.put(key, nextCount);
        if (nextCount >= maxAttempts) {
            locks.put(key, now.plus(LOCK_DURATION));
            attempts.remove(key);
        }
    }

    private void clearAttemptState(Map<String, Integer> attempts, Map<String, Instant> locks, String key) {
        attempts.remove(key);
        locks.remove(key);
    }

    private String getLockMessage(Map<String, Instant> locks, String key) {
        Instant lockedUntil = locks.get(key);
        if (lockedUntil == null) {
            return null;
        }
        if (lockedUntil.isBefore(Instant.now())) {
            locks.remove(key);
            return null;
        }
        long minutes = Math.max(1, Duration.between(Instant.now(), lockedUntil).toMinutes());
        return "Tạm khóa trong " + minutes + " phút do nhập sai nhiều lần.";
    }
}
