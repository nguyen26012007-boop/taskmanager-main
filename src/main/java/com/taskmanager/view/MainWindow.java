package com.taskmanager.view;

import com.taskmanager.dao.FolderDAO;
import com.taskmanager.controller.FolderController;
import com.taskmanager.controller.ReminderController;
import com.taskmanager.controller.TaskController;
import com.taskmanager.controller.UserController;
import com.taskmanager.model.Folder;
import com.taskmanager.model.Reminder;
import com.taskmanager.model.Task;
import com.taskmanager.model.User;
import com.taskmanager.service.ReminderService;
import com.taskmanager.util.AnimationUtil;
import com.taskmanager.util.DBConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Cửa sổ chính của ứng dụng.
 */
public class MainWindow {

    private static final DateTimeFormatter BACKUP_FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter REMINDER_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter LAST_LOGIN_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Stage stage;
    private final ReminderService reminderService;
    private final User currentUser;
    private final Runnable onLogout;

    private BorderPane root;
    private VBox sidebar;
    private StackPane contentArea;

    private DashboardView dashboardView;
    private TaskListView taskListView;
    private CalendarView calendarView;
    private ChartView chartView;

    private ToggleButton btnDashboard;
    private ToggleButton btnTasks;
    private ToggleButton btnCalendar;
    private ToggleButton btnCharts;
    private ToggleGroup navGroup;
    private Label badgeLabel;

    private int currentFolderId = FolderDAO.ALL_FOLDER_ID;

    public MainWindow(Stage stage, ReminderService reminderService, User currentUser, Runnable onLogout) {
        this.stage = stage;
        this.reminderService = reminderService;
        this.currentUser = currentUser;
        this.onLogout = onLogout;
    }

    public void show() {
        buildUI();
        setupReminderCallbacks();
        applyWindowSettings();
        navigateTo("dashboard");
        stage.show();
        AnimationUtil.fadeIn(root, 400);
        Platform.runLater(this::promptForMissingRecoveryPinIfNeeded);
    }

    private void buildUI() {
        root = new BorderPane();
        root.getStyleClass().add("root-pane");

        sidebar = buildSidebar();
        root.setLeft(sidebar);

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        root.setCenter(contentArea);

        StackPane fabContainer = buildFAB();
        root.setRight(fabContainer);

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Personal Task Manager");
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/images/logo.png");
            if (iconStream != null) {
                stage.getIcons().clear();
                stage.getIcons().add(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setOnCloseRequest(e -> saveWindowSettings());
    }

    private VBox buildSidebar() {
        VBox newSidebar = new VBox(0);
        newSidebar.getStyleClass().add("sidebar");
        newSidebar.setPrefWidth(220);
        newSidebar.setMinWidth(220);
        newSidebar.setMaxWidth(220);

        HBox logoBox = new HBox(10);
        logoBox.getStyleClass().add("sidebar-logo");
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(20, 16, 16, 16));

        StackPane logoIcon = new StackPane();
        logoIcon.getStyleClass().add("logo-icon");
        logoIcon.setPrefSize(36, 36);
        FontIcon checkIcon = new FontIcon(FontAwesomeSolid.CHECK_DOUBLE);
        checkIcon.setIconSize(16);
        checkIcon.getStyleClass().add("logo-icon-glyph");
        logoIcon.getChildren().add(checkIcon);

        VBox logoText = new VBox(2);
        Label appName = new Label("Task Manager");
        appName.getStyleClass().add("logo-title");
        Label appVersion = new Label("v1.0 Personal");
        appVersion.getStyleClass().add("logo-subtitle");
        Label copyright = new Label("© Copyright 2026 by tnguyeen");
        copyright.getStyleClass().add("logo-subtitle");
        logoText.getChildren().addAll(appName, appVersion, copyright);

        logoBox.getChildren().addAll(logoIcon, logoText);

        HBox userBox = new HBox(10);
        userBox.getStyleClass().add("sidebar-user");
        userBox.setAlignment(Pos.CENTER_LEFT);
        userBox.setPadding(new Insets(12, 16, 12, 16));
        userBox.setCursor(Cursor.HAND);

        StackPane avatarStack = new StackPane();
        Circle avatar = new Circle(18);
        avatar.getStyleClass().add("avatar-circle");
        String displayName = currentUser != null && currentUser.getDisplayName() != null
            ? currentUser.getDisplayName()
            : "Người dùng";
        String initial = displayName.isBlank() ? "U" : displayName.substring(0, 1).toUpperCase(Locale.ROOT);
        Label avatarInitial = new Label(initial);
        avatarInitial.getStyleClass().add("avatar-initial");
        avatarStack.getChildren().addAll(avatar, avatarInitial);

        VBox userInfo = new VBox(2);
        Label userName = new Label(displayName);
        userName.getStyleClass().add("user-name");
        Label userStatus = new Label("● Đang hoạt động");
        userStatus.getStyleClass().add("user-status");
        userInfo.getChildren().addAll(userName, userStatus);

        userBox.getChildren().addAll(avatarStack, userInfo);
        userBox.setOnMouseClicked(e -> showUserMenuClean(userBox));

        Separator sep1 = new Separator();
        sep1.getStyleClass().add("sidebar-separator");

        Label navLabel = new Label("MENU CHÍNH");
        navLabel.getStyleClass().add("sidebar-section-label");
        navLabel.setPadding(new Insets(12, 16, 6, 16));

        navGroup = new ToggleGroup();
        btnDashboard = createNavButton("Tổng quan", FontAwesomeSolid.HOME, "dashboard");
        btnTasks = createNavButton("Công việc", FontAwesomeSolid.TASKS, "tasks");
        btnCalendar = createNavButton("Lịch biểu", FontAwesomeSolid.CALENDAR_ALT, "calendar");
        btnCharts = createNavButton("Thống kê", FontAwesomeSolid.CHART_BAR, "charts");

        Separator sep2 = new Separator();
        sep2.getStyleClass().add("sidebar-separator");
        sep2.setPadding(new Insets(8, 0, 8, 0));

        Label foldersLabel = new Label("THƯ MỤC");
        foldersLabel.getStyleClass().add("sidebar-section-label");
        foldersLabel.setPadding(new Insets(4, 16, 6, 16));

        VBox folderList = buildFolderList();

        Button btnAddFolder = new Button("Thêm thư mục");
        btnAddFolder.getStyleClass().add("add-folder-btn");
        FontIcon plusIcon = new FontIcon(FontAwesomeSolid.PLUS);
        plusIcon.setIconSize(10);
        plusIcon.setIconColor(javafx.scene.paint.Color.web("#45678F"));
        btnAddFolder.setGraphic(plusIcon);
        btnAddFolder.setGraphicTextGap(6);
        btnAddFolder.setAlignment(Pos.CENTER_LEFT);
        btnAddFolder.setMaxWidth(Double.MAX_VALUE);
        btnAddFolder.setOnAction(e -> showCreateFolderDialog());
        VBox.setMargin(btnAddFolder, new Insets(4, 12, 4, 12));

        btnAddFolder.hoverProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                plusIcon.setIconColor(javafx.scene.paint.Color.web("#16325C"));
            } else {
                plusIcon.setIconColor(javafx.scene.paint.Color.web("#45678F"));
            }
        });

        Separator sep3 = new Separator();
        sep3.getStyleClass().add("sidebar-separator");
        sep3.setPadding(new Insets(8, 0, 8, 0));

        Label actionLabel = new Label("THAO TÁC NHANH");
        actionLabel.getStyleClass().add("sidebar-section-label");
        actionLabel.setPadding(new Insets(4, 16, 6, 16));

        Button btnQuickTask = createActionNavButton("Thêm task nhanh", FontAwesomeSolid.BOLT, this::showQuickAddTask);
        Button btnFullTask = createActionNavButton("Thêm task đầy đủ", FontAwesomeSolid.PLUS, () -> showAddTaskDialog(null));
        Button btnAddReminder = createActionNavButton("Thêm nhắc nhở", FontAwesomeSolid.BELL, this::showCreateReminderDialog);
        Button btnViewReminders = createActionNavButton("Nhắc nhở sắp tới", FontAwesomeSolid.CLOCK, this::showUpcomingRemindersDialog);

        VBox scrollContent = new VBox(0);
        scrollContent.getChildren().addAll(
            sep1,
            navLabel,
            btnDashboard, btnTasks, btnCalendar, btnCharts,
            sep2,
            foldersLabel,
            folderList,
            btnAddFolder,
            sep3,
            actionLabel,
            btnQuickTask,
            btnFullTask,
            btnAddReminder,
            btnViewReminders
        );

        ScrollPane sidebarScroll = new ScrollPane(scrollContent);
        sidebarScroll.getStyleClass().add("transparent-scroll");
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(sidebarScroll, Priority.ALWAYS);

        VBox progressBox = buildOverallProgress();
        progressBox.getStyleClass().add("sidebar-progress");

        newSidebar.getChildren().addAll(
            logoBox,
            userBox,
            sidebarScroll,
            progressBox
        );

        return newSidebar;
    }

    private Button createActionNavButton(String text, FontAwesomeSolid icon, Runnable action) {
        Button btn = new Button();
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(16);
        fontIcon.getStyleClass().add("nav-icon");

        Label label = new Label(text);
        label.getStyleClass().add("nav-label");

        HBox content = new HBox(10, fontIcon, label);
        content.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphic(content);
        btn.setOnAction(e -> action.run());

        VBox.setMargin(btn, new Insets(2, 8, 2, 8));
        return btn;
    }

    private ToggleButton createNavButton(String text, FontAwesomeSolid icon, String targetView) {
        ToggleButton btn = new ToggleButton();
        btn.getStyleClass().add("nav-button");
        btn.setToggleGroup(navGroup);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(16);
        fontIcon.getStyleClass().add("nav-icon");

        Label label = new Label(text);
        label.getStyleClass().add("nav-label");

        HBox content;
        if ("dashboard".equals(targetView)) {
            badgeLabel = new Label("0");
            badgeLabel.getStyleClass().add("nav-badge");
            badgeLabel.setVisible(false);
            Region badgeSpacer = new Region();
            HBox.setHgrow(badgeSpacer, Priority.ALWAYS);
            content = new HBox(10, fontIcon, label, badgeSpacer, badgeLabel);
        } else {
            content = new HBox(10, fontIcon, label);
        }

        content.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphic(content);
        btn.setOnAction(e -> {
            if (btn.isSelected()) {
                if ("tasks".equals(targetView)) {
                    currentFolderId = FolderDAO.ALL_FOLDER_ID;
                    if (taskListView != null) {
                        taskListView.filterByFolder(FolderDAO.ALL_FOLDER_ID);
                    }
                }
                navigateTo(targetView);
            }
        });

        VBox.setMargin(btn, new Insets(2, 8, 2, 8));
        return btn;
    }

    private VBox buildFolderList() {
        VBox folderBox = new VBox(2);
        folderBox.setPadding(new Insets(0, 8, 0, 8));

        try {
            List<Folder> folders = new FolderController().getAllFolders();
            for (Folder folder : folders) {
                folderBox.getChildren().add(createFolderItem(folder));
            }
        } catch (Exception e) {
            showToast("Không thể tải thư mục: " + e.getMessage(), "error");
        }

        return folderBox;
    }

    private HBox createFolderItem(Folder folder) {
        HBox item = new HBox(8);
        item.getStyleClass().add("folder-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(6, 10, 6, 10));
        item.setCursor(Cursor.HAND);

        Circle colorDot = new Circle(5);
        colorDot.setFill(Color.web(folder.getColor()));

        Label folderName = new Label(folder.getName());
        folderName.getStyleClass().add("folder-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label(String.valueOf(folder.getTaskCount()));
        countLabel.getStyleClass().add("folder-count");

        item.getChildren().addAll(colorDot, folderName, spacer, countLabel);
        item.setOnMouseClicked(e -> {
            currentFolderId = folder.getId();
            navigateTo("tasks");
            if (taskListView != null) {
                taskListView.filterByFolder(folder.getId());
            }
        });

        AnimationUtil.addHoverEffect(item, folder.getColor());
        return item;
    }

    private VBox buildOverallProgress() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(16));
        box.getStyleClass().add("sidebar-progress-box");

        Label title = new Label("Tiến độ tổng thể");
        title.getStyleClass().add("progress-title");

        double progress = 0;
        int percent = 0;
        try {
            int[] stats = new TaskController().getStatistics();
            if (stats[0] > 0) {
                progress = (double) stats[3] / stats[0];
                percent = (int) Math.round(progress * 100);
            }
        } catch (Exception ignored) {
        }

        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.getStyleClass().add("sidebar-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label percentLabel = new Label(percent + "% hoàn thành");
        percentLabel.getStyleClass().add("progress-percent");

        box.getChildren().addAll(title, progressBar, percentLabel);
        return box;
    }

    private StackPane buildFAB() {
        StackPane container = new StackPane();
        container.setPadding(new Insets(0, 24, 24, 0));
        container.setAlignment(Pos.BOTTOM_RIGHT);
        container.setPickOnBounds(false);

        Button fab = new Button();
        fab.getStyleClass().add("fab-button");
        FontIcon plusIcon = new FontIcon(FontAwesomeSolid.PLUS);
        plusIcon.setIconSize(22);
        plusIcon.getStyleClass().add("fab-icon");
        fab.setGraphic(plusIcon);

        ContextMenu fabMenu = new ContextMenu();
        fabMenu.getStyleClass().add("fab-menu");

        MenuItem quickTask = new MenuItem("Thêm task nhanh");
        quickTask.setOnAction(e -> showQuickAddTask());

        MenuItem fullTask = new MenuItem("Thêm task đầy đủ");
        fullTask.setOnAction(e -> showAddTaskDialog(null));

        MenuItem addReminder = new MenuItem("Thêm nhắc nhở");
        addReminder.setOnAction(e -> showCreateReminderDialog());

        MenuItem viewReminders = new MenuItem("Nhắc nhở sắp tới");
        viewReminders.setOnAction(e -> showUpcomingRemindersDialog());

        fabMenu.getItems().addAll(quickTask, fullTask, addReminder, viewReminders);

        fab.setOnAction(e -> {
            fabMenu.show(fab, Side.TOP, 0, 0);
            AnimationUtil.pulse(fab);
        });

        container.getChildren().add(fab);
        return container;
    }

    public void navigateTo(String view) {
        contentArea.getChildren().clear();

        switch (view) {
            case "dashboard" -> {
                if (dashboardView == null) {
                    dashboardView = new DashboardView(this);
                }
                dashboardView.refresh();
                contentArea.getChildren().add(dashboardView.getRoot());
                btnDashboard.setSelected(true);
                AnimationUtil.slideInFromBottom(dashboardView.getRoot(), 300);
            }
            case "tasks" -> {
                if (taskListView == null) {
                    taskListView = new TaskListView(this);
                }
                contentArea.getChildren().add(taskListView.getRoot());
                btnTasks.setSelected(true);
                taskListView.refresh();
                AnimationUtil.slideInFromBottom(taskListView.getRoot(), 300);
            }
            case "calendar" -> {
                if (calendarView == null) {
                    calendarView = new CalendarView(this);
                }
                contentArea.getChildren().add(calendarView.getRoot());
                btnCalendar.setSelected(true);
                calendarView.refresh();
                AnimationUtil.slideInFromBottom(calendarView.getRoot(), 300);
            }
            case "charts" -> {
                if (chartView == null) {
                    chartView = new ChartView(this);
                }
                contentArea.getChildren().add(chartView.getRoot());
                btnCharts.setSelected(true);
                chartView.refresh();
                AnimationUtil.slideInFromBottom(chartView.getRoot(), 300);
            }
            default -> navigateTo("dashboard");
        }
    }

    public void showAddTaskDialog(Task existingTask) {
        TaskDialog dialog = new TaskDialog(stage, existingTask);
        dialog.showAndWait().ifPresent(task -> refreshAllDataViews());
    }

    private void showQuickAddTask() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm task nhanh");
        dialog.setHeaderText(null);
        dialog.setContentText("Tên công việc:");
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        dialog.showAndWait().ifPresent(title -> {
            if (title == null || title.trim().isEmpty()) {
                return;
            }

            Task task = new Task();
            task.setTitle(title.trim());
            task.setStatus(Task.Status.TODO);
            task.setPriority(Task.Priority.MEDIUM);

            if (currentFolderId == FolderDAO.TODAY_FOLDER_ID) {
                task.setDueDate(LocalDate.now());
            } else if (currentFolderId == FolderDAO.IMPORTANT_FOLDER_ID) {
                task.setPriority(Task.Priority.HIGH);
            } else if (currentFolderId > 0) {
                task.setFolderId(currentFolderId);
            }

            try {
                new TaskController().insert(task);
                refreshAllDataViews();
                showToast("Đã thêm task: " + title.trim(), "success");
            } catch (Exception e) {
                showToast("Không thể thêm task: " + e.getMessage(), "error");
            }
        });
    }

    private void showCreateFolderDialog() {
        Dialog<Folder> dialog = new Dialog<>();
        dialog.setTitle("Tạo thư mục mới");
        dialog.initOwner(stage);

        TextField nameField = new TextField();
        nameField.setPromptText("Tên thư mục...");
        nameField.getStyleClass().add("form-field");

        VBox content = new VBox(12, new Label("Tên thư mục"), nameField);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Tạo");

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                Folder folder = new Folder();
                folder.setName(nameField.getText().trim());
                folder.setColor("#E94560");
                folder.setIcon("folder");
                return folder;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(folder -> {
            try {
                new FolderController().insert(folder);
                refreshSidebar();
                showToast("Đã tạo thư mục: " + folder.getName(), "success");
            } catch (Exception e) {
                showToast("Không thể tạo thư mục: " + e.getMessage(), "error");
            }
        });
    }

    private void showUserMenuClean(HBox userBox) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("context-menu-dark");

        MenuItem infoItem = new MenuItem("Thông tin người dùng");
        infoItem.setOnAction(e -> showUserInfoDialog());

        MenuItem settingsItem = new MenuItem("Cài đặt thông tin");
        settingsItem.setOnAction(e -> showAccountSettingsDialog());

        MenuItem recoveryPinItem = new MenuItem("Thiết lập mã PIN khôi phục");
        recoveryPinItem.setOnAction(e -> showRecoveryPinDialogClean());

        MenuItem upcomingReminderItem = new MenuItem("Nhắc nhở sắp tới");
        upcomingReminderItem.setOnAction(e -> showUpcomingRemindersDialog());

        MenuItem backupItem = new MenuItem("Sao lưu dữ liệu");
        backupItem.setOnAction(e -> backupDatabase());

        MenuItem restoreItem = new MenuItem("Khôi phục dữ liệu");
        restoreItem.setOnAction(e -> restoreDatabase());

        menu.getItems().addAll(infoItem, settingsItem, recoveryPinItem, upcomingReminderItem, backupItem, restoreItem);

        if (currentUser != null && currentUser.isAdmin()) {
            MenuItem manageUsersItem = new MenuItem("Quản lý người dùng");
            manageUsersItem.setOnAction(e -> showAdminUserManagementDialogClean());

            MenuItem adminRecoveryPinItem = new MenuItem("Admin đặt lại mã PIN user");
            adminRecoveryPinItem.setOnAction(e -> showAdminRecoveryPinDialogClean());

            menu.getItems().addAll(manageUsersItem, adminRecoveryPinItem);
        }

        MenuItem guideItem = new MenuItem("Hướng dẫn sử dụng app");
        guideItem.setOnAction(e -> showUserGuideDialog());

        MenuItem logoutItem = new MenuItem("Đăng xuất");
        logoutItem.getStyleClass().add("danger");
        logoutItem.setOnAction(e -> performLogout());

        menu.getItems().addAll(new SeparatorMenuItem(), guideItem, new SeparatorMenuItem(), logoutItem);
        menu.show(userBox, Side.BOTTOM, 0, 6);
    }

    private void showUserInfoDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Thông tin người dùng");
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        VBox content = new VBox(14);
        content.setPadding(new Insets(18));

        Label title = new Label("Hồ sơ hiện tại");
        title.getStyleClass().add("section-title");

        content.getChildren().addAll(
            title,
            infoLine("Tên hiển thị", safeText(currentUser.getDisplayName())),
            infoLine("Tên đăng nhập", safeText(currentUser.getUsername())),
            infoLine("Vai trò", currentUser.isAdmin() ? "Admin" : "Người dùng"),
            infoLine("Mã người dùng", "#" + currentUser.getId()),
            infoLine("Mã PIN khôi phục", currentUser.hasRecoveryPin() ? "Đã thiết lập" : "Chưa thiết lập"),
            infoLine("Lần đăng nhập gần nhất", formatLastLogin(currentUser.getLastLoginAt()))
        );

        Label note = new Label("Bạn có thể mở mục Cài đặt thông tin để đổi tên, đổi mật khẩu hoặc thiết lập mã PIN khôi phục.");
        note.getStyleClass().add("empty-state-subtitle");
        note.setWrapText(true);
        content.getChildren().add(note);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private HBox infoLine(String labelText, String valueText) {
        Label label = new Label(labelText + ":");
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        Label value = new Label(valueText);
        value.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, label, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox infoLineNode(String labelText, Label valueLabel) {
        Label label = new Label(labelText + ":");
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        valueLabel.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, label, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void showAccountSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Cài đặt thông tin");
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        TextField displayNameField = new TextField(currentUser.getDisplayName());
        displayNameField.getStyleClass().add("form-field");
        displayNameField.setPromptText("Tên hiển thị");

        Label usernameHint = new Label("Tên đăng nhập: " + safeText(currentUser.getUsername()));
        usernameHint.getStyleClass().add("page-subtitle");

        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.getStyleClass().add("form-field");
        currentPasswordField.setPromptText("Mật khẩu hiện tại");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.getStyleClass().add("form-field");
        newPasswordField.setPromptText("Mật khẩu mới");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.getStyleClass().add("form-field");
        confirmPasswordField.setPromptText("Nhập lại mật khẩu mới");

        PasswordField recoveryPinField = new PasswordField();
        recoveryPinField.getStyleClass().add("form-field");
        recoveryPinField.setPromptText("Mã PIN 6 số mới");

        PasswordField confirmRecoveryPinField = new PasswordField();
        confirmRecoveryPinField.getStyleClass().add("form-field");
        confirmRecoveryPinField.setPromptText("Nhập lại mã PIN 6 số");

        Label note = new Label(
            "Nếu bạn muốn đổi mật khẩu hoặc đổi mã PIN, hãy nhập mật khẩu hiện tại. "
                + "Bạn có thể chỉ đổi tên hiển thị mà không cần nhập mật khẩu."
        );
        note.getStyleClass().add("empty-state-subtitle");
        note.setWrapText(true);

        VBox content = new VBox(12,
            new Label("Tên hiển thị"), displayNameField,
            usernameHint,
            new Separator(),
            new Label("Mật khẩu hiện tại"), currentPasswordField,
            new Label("Mật khẩu mới"), newPasswordField,
            new Label("Xác nhận mật khẩu mới"), confirmPasswordField,
            new Separator(),
            new Label("Mã PIN khôi phục mới"), recoveryPinField,
            new Label("Xác nhận mã PIN khôi phục"), confirmRecoveryPinField,
            note
        );
        content.setPadding(new Insets(18));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        saveButton.setText("Lưu thay đổi");
        saveButton.getStyleClass().add("primary-button");

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("secondary-button");

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }

            String newDisplayName = displayNameField.getText() != null ? displayNameField.getText().trim() : "";
            String currentPassword = valueOf(currentPasswordField);
            String newPassword = valueOf(newPasswordField);
            String confirmPassword = valueOf(confirmPasswordField);
            String newPin = valueOf(recoveryPinField).trim();
            String confirmPin = valueOf(confirmRecoveryPinField).trim();

            if (newDisplayName.isEmpty()) {
                showToast("Tên hiển thị không được để trống", "error");
                return;
            }

            boolean changingPassword = !newPassword.isBlank() || !confirmPassword.isBlank();
            boolean changingPin = !newPin.isBlank() || !confirmPin.isBlank();
            if ((changingPassword || changingPin) && currentPassword.isBlank()) {
                showToast("Hãy nhập mật khẩu hiện tại để đổi mật khẩu hoặc mã PIN", "error");
                return;
            }
            if (changingPassword) {
                if (newPassword.length() < 6) {
                    showToast("Mật khẩu mới cần ít nhất 6 ký tự", "error");
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    showToast("Mật khẩu xác nhận không khớp", "error");
                    return;
                }
            }
            if (changingPin) {
                if (!newPin.matches("\\d{6}")) {
                    showToast("Mã PIN phải đúng 6 chữ số", "error");
                    return;
                }
                if (!newPin.equals(confirmPin)) {
                    showToast("Mã PIN xác nhận không khớp", "error");
                    return;
                }
            }

            try {
                UserController userDAO = new UserController();
                userDAO.updateProfile(newDisplayName);
                currentUser.setName(newDisplayName);

                if (changingPassword) {
                    boolean passwordChanged = userDAO.changePassword(currentPassword, newPassword);
                    if (!passwordChanged) {
                        showToast("Mật khẩu hiện tại không đúng", "error");
                        return;
                    }
                }

                if (changingPin) {
                    boolean pinChanged = userDAO.updateRecoveryPin(currentPassword, newPin);
                    if (!pinChanged) {
                        showToast("Không thể cập nhật mã PIN. Kiểm tra lại mật khẩu hiện tại", "error");
                        return;
                    }
                    currentUser.setRecoveryPinHash("UPDATED");
                }

                refreshSidebar();
                showToast("Đã cập nhật thông tin người dùng", "success");
            } catch (Exception ex) {
                showToast("Không thể cập nhật thông tin: " + ex.getMessage(), "error");
            }
        });
    }

    private void showRecoveryPinDialogClean() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thiết lập mã PIN khôi phục");
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.getStyleClass().add("form-field");
        currentPasswordField.setPromptText("Mật khẩu hiện tại");

        PasswordField recoveryPinField = new PasswordField();
        recoveryPinField.getStyleClass().add("form-field");
        recoveryPinField.setPromptText("Mã PIN 6 số");

        PasswordField confirmRecoveryPinField = new PasswordField();
        confirmRecoveryPinField.getStyleClass().add("form-field");
        confirmRecoveryPinField.setPromptText("Nhập lại mã PIN 6 số");

        Label note = new Label("Mã PIN 6 số dùng để khôi phục mật khẩu nếu bạn quên mật khẩu đăng nhập.");
        note.getStyleClass().add("empty-state-subtitle");
        note.setWrapText(true);

        VBox content = new VBox(12,
            new Label("Mật khẩu hiện tại"), currentPasswordField,
            new Label("Mã PIN khôi phục"), recoveryPinField,
            new Label("Xác nhận mã PIN"), confirmRecoveryPinField,
            note
        );
        content.setPadding(new Insets(18));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        saveButton.setText("Lưu mã PIN");
        saveButton.getStyleClass().add("primary-button");

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }

            String currentPassword = valueOf(currentPasswordField);
            String recoveryPin = valueOf(recoveryPinField).trim();
            String confirmRecoveryPin = valueOf(confirmRecoveryPinField).trim();

            if (currentPassword.isBlank()) {
                showToast("Hãy nhập mật khẩu hiện tại", "error");
                return;
            }
            if (!recoveryPin.matches("\\d{6}")) {
                showToast("Mã PIN phải đúng 6 chữ số", "error");
                return;
            }
            if (!recoveryPin.equals(confirmRecoveryPin)) {
                showToast("Mã PIN xác nhận không khớp", "error");
                return;
            }

            try {
                boolean pinChanged = new UserController().updateRecoveryPin(currentPassword, recoveryPin);
                if (!pinChanged) {
                    showToast("Không thể lưu mã PIN. Kiểm tra lại mật khẩu hiện tại", "error");
                    return;
                }
                currentUser.setRecoveryPinHash("UPDATED");
                refreshSidebar();
                showToast("Đã cập nhật mã PIN khôi phục", "success");
            } catch (Exception ex) {
                showToast("Lỗi cập nhật mã PIN: " + ex.getMessage(), "error");
            }
        });
    }

    private void showAdminRecoveryPinDialogClean() {
        if (currentUser == null || !currentUser.isAdmin()) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Admin đặt lại mã PIN người dùng");
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        ComboBox<User> userCombo = new ComboBox<>();
        userCombo.getStyleClass().add("form-combo");
        userCombo.setMaxWidth(Double.MAX_VALUE);

        try {
            userCombo.getItems().addAll(new UserController().getAllUsers().stream()
                .filter(user -> user.getId() != currentUser.getId())
                .toList());
        } catch (Exception e) {
            showToast("Không thể tải danh sách người dùng", "error");
            return;
        }

        userCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName() + " (" + item.getUsername() + ")");
            }
        });
        userCombo.setButtonCell(userCombo.getCellFactory().call(null));

        PasswordField recoveryPinField = new PasswordField();
        recoveryPinField.getStyleClass().add("form-field");
        recoveryPinField.setPromptText("Mã PIN 6 số mới");

        PasswordField confirmRecoveryPinField = new PasswordField();
        confirmRecoveryPinField.getStyleClass().add("form-field");
        confirmRecoveryPinField.setPromptText("Nhập lại mã PIN 6 số");

        Label note = new Label("Vì mã PIN được lưu ở dạng bảo mật, admin không thể xem mã PIN cũ mà chỉ có thể đặt một mã PIN mới.");
        note.getStyleClass().add("empty-state-subtitle");
        note.setWrapText(true);

        VBox content = new VBox(12,
            new Label("Người dùng"), userCombo,
            new Label("Mã PIN mới"), recoveryPinField,
            new Label("Xác nhận mã PIN"), confirmRecoveryPinField,
            note
        );
        content.setPadding(new Insets(18));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        saveButton.setText("Đặt lại mã PIN");
        saveButton.getStyleClass().add("primary-button");

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }

            User selectedUser = userCombo.getValue();
            String recoveryPin = valueOf(recoveryPinField).trim();
            String confirmRecoveryPin = valueOf(confirmRecoveryPinField).trim();

            if (selectedUser == null) {
                showToast("Hãy chọn người dùng cần đặt lại mã PIN", "error");
                return;
            }
            if (!recoveryPin.matches("\\d{6}")) {
                showToast("Mã PIN phải đúng 6 chữ số", "error");
                return;
            }
            if (!recoveryPin.equals(confirmRecoveryPin)) {
                showToast("Mã PIN xác nhận không khớp", "error");
                return;
            }

            try {
                new UserController().adminResetPin(selectedUser.getId(), recoveryPin);
                showToast("Đã đặt lại mã PIN cho " + selectedUser.getDisplayName(), "success");
            } catch (Exception ex) {
                showToast("Không thể đặt lại mã PIN: " + ex.getMessage(), "error");
            }
        });
    }

    private void showAdminUserManagementDialogClean() {
        if (currentUser == null || !currentUser.isAdmin()) {
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Quản lý người dùng");
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        dialog.getDialogPane().setPrefWidth(940);
        dialog.getDialogPane().setPrefHeight(620);

        ObservableList<User> masterUsers = FXCollections.observableArrayList();
        try {
            masterUsers.setAll(new UserController().getAllUsers());
        } catch (Exception e) {
            showToast("Không thể tải danh sách người dùng", "error");
            return;
        }

        TextField searchField = new TextField();
        searchField.getStyleClass().add("form-field");
        searchField.setPromptText("Tìm theo tên hoặc username...");

        ComboBox<String> roleFilter = new ComboBox<>();
        roleFilter.getItems().addAll("Tất cả vai trò", "Chỉ admin", "Chỉ người dùng");
        roleFilter.setValue("Tất cả vai trò");
        roleFilter.getStyleClass().add("form-combo");

        ComboBox<String> pinFilter = new ComboBox<>();
        pinFilter.getItems().addAll("Tất cả PIN", "Đã có PIN", "Chưa có PIN");
        pinFilter.setValue("Tất cả PIN");
        pinFilter.getStyleClass().add("form-combo");

        HBox filterBar = new HBox(10, searchField, roleFilter, pinFilter);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        ListView<User> userList = new ListView<>();
        userList.getStyleClass().add("task-table");

        TextField displayNameField = new TextField();
        displayNameField.getStyleClass().add("form-field");
        displayNameField.setPromptText("Tên hiển thị");

        CheckBox adminCheckBox = new CheckBox("Quyền admin");

        PasswordField resetPasswordField = new PasswordField();
        resetPasswordField.getStyleClass().add("form-field");
        resetPasswordField.setPromptText("Mật khẩu mới nếu muốn đặt lại");

        PasswordField resetPinField = new PasswordField();
        resetPinField.getStyleClass().add("form-field");
        resetPinField.setPromptText("Mã PIN 6 số mới nếu muốn đặt lại");

        Label usernameValue = new Label("Chưa chọn");
        Label roleValue = new Label("Chưa chọn");
        Label pinValue = new Label("Chưa chọn");
        Label lastLoginValue = new Label("Chưa chọn");

        Runnable reloadList = () -> {
            String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
            String selectedRole = roleFilter.getValue();
            String selectedPin = pinFilter.getValue();

            List<User> filtered = masterUsers.stream()
                .filter(user -> query.isBlank()
                    || user.getDisplayName().toLowerCase(Locale.ROOT).contains(query)
                    || safeText(user.getUsername()).toLowerCase(Locale.ROOT).contains(query))
                .filter(user -> switch (selectedRole) {
                    case "Chỉ admin" -> user.isAdmin();
                    case "Chỉ người dùng" -> !user.isAdmin();
                    default -> true;
                })
                .filter(user -> switch (selectedPin) {
                    case "Đã có PIN" -> user.hasRecoveryPin();
                    case "Chưa có PIN" -> !user.hasRecoveryPin();
                    default -> true;
                })
                .sorted(Comparator.comparing(User::isAdmin).reversed()
                    .thenComparing(User::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
            userList.getItems().setAll(filtered);
        };

        userList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String role = item.isAdmin() ? "Admin" : "Người dùng";
                String pinStatus = item.hasRecoveryPin() ? "Đã có PIN" : "Chưa có PIN";
                setText(item.getDisplayName() + " (" + item.getUsername() + ") - " + role + " - " + pinStatus);
            }
        });

        userList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                displayNameField.clear();
                adminCheckBox.setSelected(false);
                usernameValue.setText("Chưa chọn");
                roleValue.setText("Chưa chọn");
                pinValue.setText("Chưa chọn");
                lastLoginValue.setText("Chưa chọn");
                resetPasswordField.clear();
                resetPinField.clear();
            } else {
                displayNameField.setText(newVal.getDisplayName());
                adminCheckBox.setSelected(newVal.isAdmin());
                usernameValue.setText(safeText(newVal.getUsername()));
                roleValue.setText(newVal.isAdmin() ? "Admin" : "Người dùng");
                pinValue.setText(newVal.hasRecoveryPin() ? "Đã thiết lập" : "Chưa thiết lập");
                lastLoginValue.setText(formatLastLogin(newVal.getLastLoginAt()));
                resetPasswordField.clear();
                resetPinField.clear();
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> reloadList.run());
        roleFilter.valueProperty().addListener((obs, oldVal, newVal) -> reloadList.run());
        pinFilter.valueProperty().addListener((obs, oldVal, newVal) -> reloadList.run());
        reloadList.run();

        Button saveButton = new Button("Lưu thay đổi");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(e -> {
            User selected = userList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showToast("Hãy chọn người dùng", "error");
                return;
            }

            String newDisplayName = displayNameField.getText() != null ? displayNameField.getText().trim() : "";
            String newPassword = valueOf(resetPasswordField);
            String newPin = valueOf(resetPinField).trim();

            if (newDisplayName.isEmpty()) {
                showToast("Tên hiển thị không được để trống", "error");
                return;
            }
            if (!newPassword.isBlank() && newPassword.length() < 6) {
                showToast("Mật khẩu mới cần ít nhất 6 ký tự", "error");
                return;
            }
            if (!newPin.isBlank() && !newPin.matches("\\d{6}")) {
                showToast("Mã PIN mới phải đúng 6 chữ số", "error");
                return;
            }

            try {
                UserController userDAO = new UserController();
                if (selected.isAdmin() && !adminCheckBox.isSelected() && userDAO.countAdmins() <= 1) {
                    showToast("Không thể bỏ quyền admin cuối cùng", "error");
                    return;
                }

                userDAO.adminUpdateUser(selected.getId(), newDisplayName, adminCheckBox.isSelected());
                if (!newPassword.isBlank()) {
                    userDAO.adminResetPassword(selected.getId(), newPassword);
                }
                if (!newPin.isBlank()) {
                    userDAO.adminResetPin(selected.getId(), newPin);
                }

                masterUsers.setAll(userDAO.getAllUsers());
                reloadList.run();
                showToast("Đã cập nhật tài khoản người dùng", "success");
            } catch (Exception ex) {
                showToast("Không thể cập nhật người dùng: " + ex.getMessage(), "error");
            }
        });

        Button deleteButton = new Button("Xóa tài khoản");
        deleteButton.getStyleClass().addAll("secondary-button", "danger");
        deleteButton.setOnAction(e -> {
            User selected = userList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showToast("Hãy chọn người dùng", "error");
                return;
            }
            if (selected.getId() == currentUser.getId()) {
                showToast("Không thể xóa tài khoản đang đăng nhập", "error");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.initOwner(stage);
            confirm.setTitle("Xác nhận xóa tài khoản");
            confirm.setHeaderText("Bạn có chắc muốn xóa " + selected.getDisplayName() + "?");
            confirm.setContentText("Toàn bộ dữ liệu gắn với tài khoản này có thể bị mất.");
            confirm.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            try {
                UserController userDAO = new UserController();
                if (selected.isAdmin() && userDAO.countAdmins() <= 1) {
                     showToast("Không thể xóa admin cuối cùng", "error");
                     return;
                }
                userDAO.deleteUser(selected.getId());
                masterUsers.setAll(userDAO.getAllUsers());
                reloadList.run();
                userList.getSelectionModel().clearSelection();
                showToast("Đã xóa tài khoản người dùng", "success");
            } catch (Exception ex) {
                showToast("Không thể xóa người dùng: " + ex.getMessage(), "error");
            }
        });

        HBox actionRow = new HBox(10, saveButton, deleteButton);

        VBox detailsBox = new VBox(12,
            filterBar,
            new Separator(),
            new Label("Tên hiển thị"), displayNameField,
            adminCheckBox,
            new Label("Đặt lại mật khẩu"), resetPasswordField,
            new Label("Đặt lại mã PIN"), resetPinField,
            new Separator(),
            infoLineNode("Username", usernameValue),
            infoLineNode("Vai trò", roleValue),
            infoLineNode("PIN khôi phục", pinValue),
            infoLineNode("Lần đăng nhập gần nhất", lastLoginValue),
            actionRow
        );
        detailsBox.setPadding(new Insets(16));

        HBox layout = new HBox(16, userList, detailsBox);
        HBox.setHgrow(userList, Priority.ALWAYS);
        HBox.setHgrow(detailsBox, Priority.ALWAYS);
        userList.setPrefWidth(380);

        dialog.getDialogPane().setContent(layout);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showUserGuideDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Hướng dẫn sử dụng app");
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(18));

        Label title = new Label("Bắt đầu nhanh cho người mới");
        title.getStyleClass().add("section-title");

        Text intro = new Text("""
1. Đăng nhập hoặc tạo tài khoản để bắt đầu sử dụng.
2. Vào mục Công việc để tạo task đầu tiên.
3. Dùng nút + Thêm task ở góc trên hoặc nút tròn dấu + ở góc dưới phải để thêm nhanh công việc.
4. Điền các thông tin cơ bản như tiêu đề, mô tả, ưu tiên, ngày bắt đầu, ngày đến hạn và thư mục nếu cần.
5. Khi đã có task, bạn có thể Sửa, Xóa hoặc Hoàn thành trực tiếp ngay trên từng task.
6. Dùng thanh tìm kiếm và bộ lọc để tìm lại công việc theo trạng thái, ưu tiên hoặc thời gian.
7. Chọn các mục ở sidebar như Tất cả, Hôm nay, Quan trọng, Hoàn thành để xem đúng nhóm công việc bạn cần.
8. Mở Lịch biểu nếu bạn muốn xem công việc theo ngày và theo khung giờ.
9. Mở Thống kê để theo dõi tiến độ, tỷ lệ hoàn thành và xu hướng làm việc.
10. Bấm vào khu vực người dùng ở sidebar để xem thông tin cá nhân, đổi tên, đổi mật khẩu, thiết lập PIN, sao lưu dữ liệu hoặc đăng xuất.
        """);
        intro.getStyleClass().add("empty-state-subtitle");

        VBox sections = new VBox(8,
            new Label("Các màn hình chính"),
            new Label("• Tổng quan: xem nhanh số lượng việc, tiến độ và các điểm cần chú ý."),
            new Label("• Công việc: nơi tạo, sửa, xóa, hoàn thành và lọc task."),
            new Label("• Lịch biểu: xem task theo ngày, chọn ngày và thêm việc nhanh."),
            new Label("• Thống kê: xem số liệu hoàn thành và hiệu suất làm việc.")
        );
        sections.getStyleClass().add("page-subtitle");

        VBox tips = new VBox(8,
            new Label("Mẹo sử dụng"),
            new Label("• Nếu chưa quen, hãy dùng mục Tất cả để nhìn toàn bộ công việc trước khi lọc sâu hơn."),
            new Label("• Nên thiết lập mã PIN khôi phục ngay sau khi tạo tài khoản để tránh bị khóa khi quên mật khẩu."),
            new Label("• Dùng nhắc nhở sắp tới để không bỏ lỡ việc quan trọng."),
            new Label("• Hãy sao lưu dữ liệu định kỳ nếu bạn có nhiều task quan trọng.")
        );
        tips.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(title, intro, sections, tips);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showCreateReminderDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thêm nhắc nhở");
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        List<Task> tasks;
        try {
            tasks = new TaskController().getAllTasks(null, null, null);
        } catch (Exception e) {
            showToast("Không thể tải danh sách task: " + e.getMessage(), "error");
            return;
        }

        if (tasks.isEmpty()) {
            showToast("Hãy tạo ít nhất một task trước khi thêm nhắc nhở", "warning");
            return;
        }

        ComboBox<Task> taskCombo = new ComboBox<>();
        taskCombo.getItems().addAll(tasks);
        taskCombo.getStyleClass().add("form-combo");
        taskCombo.setMaxWidth(Double.MAX_VALUE);
        taskCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        taskCombo.setButtonCell(taskCombo.getCellFactory().call(null));

        ComboBox<String> presetCombo = new ComboBox<>();
        presetCombo.getItems().addAll("Tùy chọn", "Sau 5 phút", "Sau 15 phút", "Sau 30 phút", "Sau 1 giờ", "Sáng mai 08:00");
        presetCombo.setValue("Tùy chọn");
        presetCombo.getStyleClass().add("form-combo");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("form-datepicker");

        TextField timeField = new TextField("09:00");
        timeField.getStyleClass().add("form-field");
        timeField.setPromptText("HH:mm");

        ComboBox<String> repeatCombo = new ComboBox<>();
        repeatCombo.getItems().addAll("Không lặp", "Hàng ngày", "Hàng tuần");
        repeatCombo.setValue("Không lặp");
        repeatCombo.getStyleClass().add("form-combo");

        presetCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            LocalDateTime base = LocalDateTime.now();
            switch (newVal) {
                case "Sau 5 phút" -> {
                    LocalDateTime target = base.plusMinutes(5);
                    datePicker.setValue(target.toLocalDate());
                    timeField.setText(target.toLocalTime().withSecond(0).withNano(0).toString());
                }
                case "Sau 15 phút" -> {
                    LocalDateTime target = base.plusMinutes(15);
                    datePicker.setValue(target.toLocalDate());
                    timeField.setText(target.toLocalTime().withSecond(0).withNano(0).toString());
                }
                case "Sau 30 phút" -> {
                    LocalDateTime target = base.plusMinutes(30);
                    datePicker.setValue(target.toLocalDate());
                    timeField.setText(target.toLocalTime().withSecond(0).withNano(0).toString());
                }
                case "Sau 1 giờ" -> {
                    LocalDateTime target = base.plusHours(1);
                    datePicker.setValue(target.toLocalDate());
                    timeField.setText(target.toLocalTime().withSecond(0).withNano(0).toString());
                }
                case "Sáng mai 08:00" -> {
                    datePicker.setValue(LocalDate.now().plusDays(1));
                    timeField.setText("08:00");
                }
                default -> {
                }
            }
        });

        Label note = new Label("Bạn có thể dùng preset nhanh hoặc nhập ngày giờ thủ công theo định dạng HH:mm.");
        note.getStyleClass().add("empty-state-subtitle");
        note.setWrapText(true);

        VBox content = new VBox(12,
            new Label("Chọn task"), taskCombo,
            new Label("Thiết lập nhanh"), presetCombo,
            new Label("Ngày nhắc"), datePicker,
            new Label("Giờ nhắc"), timeField,
            new Label("Lặp lại"), repeatCombo,
            note
        );
        content.setPadding(new Insets(18));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        saveButton.setText("Lưu nhắc nhở");
        saveButton.getStyleClass().add("primary-button");

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }

            Task selectedTask = taskCombo.getValue();
            if (selectedTask == null) {
                showToast("Hãy chọn một task", "error");
                return;
            }

            LocalDate date = datePicker.getValue();
            if (date == null) {
                showToast("Hãy chọn ngày nhắc", "error");
                return;
            }

            LocalDateTime remindAt;
            try {
                remindAt = LocalDateTime.parse(date + "T" + normalizeTime(timeField.getText()));
            } catch (Exception e) {
                showToast("Giờ nhắc phải đúng định dạng HH:mm", "error");
                return;
            }

            if (remindAt.isBefore(LocalDateTime.now().minusMinutes(1))) {
                showToast("Thời điểm nhắc phải ở hiện tại hoặc tương lai", "error");
                return;
            }

            Reminder reminder = new Reminder();
            reminder.setTaskId(selectedTask.getId());
            reminder.setTaskTitle(selectedTask.getTitle());
            reminder.setRemindAt(remindAt);
            reminder.setRepeatType(parseRepeatType(repeatCombo.getValue()));

            try {
                new ReminderController().insert(reminder);
                showToast("Đã thêm nhắc nhở cho task: " + selectedTask.getTitle(), "success");
            } catch (Exception e) {
                showToast("Không thể lưu nhắc nhở: " + e.getMessage(), "error");
            }
        });
    }

    private void showUpcomingRemindersDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Nhắc nhở sắp tới");
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        dialog.getDialogPane().setPrefWidth(640);
        dialog.getDialogPane().setPrefHeight(500);

        ListView<Reminder> reminderList = new ListView<>();
        reminderList.getStyleClass().add("task-table");

        Runnable reload = () -> {
            try {
                reminderList.getItems().setAll(new ReminderController().getUpcomingReminders(50));
            } catch (Exception e) {
                showToast("Không thể tải nhắc nhở: " + e.getMessage(), "error");
            }
        };
        reload.run();

        reminderList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Reminder item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String repeatText = switch (item.getRepeatType()) {
                    case DAILY -> "Hàng ngày";
                    case WEEKLY -> "Hàng tuần";
                    default -> "Không lặp";
                };
                String statusText = item.isSent() ? "Đã gửi" : "Chưa gửi";
                setText(item.getTaskTitle() + " - " + item.getRemindAt().format(REMINDER_FORMAT) + " - " + repeatText + " - " + statusText);
            }
        });

        Label helper = new Label("Bạn có thể xóa các nhắc nhở không còn cần thiết. Các reminder lặp lại sẽ tự dời sang lần kế tiếp sau khi kích hoạt.");
        helper.getStyleClass().add("empty-state-subtitle");
        helper.setWrapText(true);

        Button addButton = new Button("Thêm nhắc nhở");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(e -> {
            showCreateReminderDialog();
            reload.run();
        });

        Button deleteButton = new Button("Xóa nhắc nhở đã chọn");
        deleteButton.getStyleClass().add("secondary-button");
        deleteButton.setOnAction(e -> {
            Reminder selected = reminderList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showToast("Hãy chọn nhắc nhở cần xóa", "error");
                return;
            }
            try {
                new ReminderController().delete(selected.getId());
                reload.run();
                showToast("Đã xóa nhắc nhở", "success");
            } catch (Exception ex) {
                showToast("Không thể xóa nhắc nhở: " + ex.getMessage(), "error");
            }
        });

        HBox actions = new HBox(10, addButton, deleteButton);
        VBox content = new VBox(12, helper, reminderList, actions);
        content.setPadding(new Insets(18));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void backupDatabase() {
        if (DBConnection.getDatabasePath().startsWith("mariadb://")) {
            showToast("Da chuyen sang MySQL/MariaDB. Hay sao luu bang HeidiSQL hoac mysqldump.", "warning");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sao lưu dữ liệu");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
        chooser.setInitialFileName("taskmanager-backup-" + LocalDateTime.now().format(BACKUP_FILE_FORMAT) + ".db");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try (Statement stmt = DBConnection.getConnection().createStatement()) {
            stmt.execute("PRAGMA wal_checkpoint(FULL)");
            if (file.exists()) {
                Files.delete(file.toPath());
            }
            stmt.execute("VACUUM INTO '" + file.getAbsolutePath().replace("\\", "/").replace("'", "''") + "'");
            showToast("Đã sao lưu dữ liệu: " + file.getName(), "success");
        } catch (Exception e) {
            showToast("Không thể sao lưu dữ liệu: " + e.getMessage(), "error");
        }
    }

    private void restoreDatabase() {
        if (DBConnection.getDatabasePath().startsWith("mariadb://")) {
            showToast("Da chuyen sang MySQL/MariaDB. Hay khoi phuc bang HeidiSQL hoac mysql client.", "warning");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Khôi phục dữ liệu");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Khôi phục dữ liệu");
        confirm.setHeaderText("Khôi phục từ file " + file.getName() + "?");
        confirm.setContentText("Dữ liệu hiện tại sẽ bị thay thế bằng bản sao lưu đã chọn.");
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            reminderService.clearBadge();
            DBConnection.close();

            Path source = file.toPath();
            Path target = Path.of(DBConnection.getDatabasePath());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(Path.of(DBConnection.getDatabasePath() + "-wal"));
            Files.deleteIfExists(Path.of(DBConnection.getDatabasePath() + "-shm"));

            DBConnection.initialize();

            User restoredUser = null;
            if (restoredUser == null) {
                showToast("Bản sao lưu không còn tài khoản hiện tại. Ứng dụng sẽ quay về màn hình đăng nhập.", "warning");
                performLogout();
                return;
            }

            currentUser.setName(restoredUser.getName());
            currentUser.setUsername(restoredUser.getUsername());
            currentUser.setAdmin(restoredUser.isAdmin());
            currentUser.setRecoveryPinHash(restoredUser.getRecoveryPinHash());
            currentUser.setLastLoginAt(restoredUser.getLastLoginAt());

            refreshAllDataViews();
            navigateTo("dashboard");
            showToast("Đã khôi phục dữ liệu thành công", "success");
        } catch (Exception e) {
            showToast("Không thể khôi phục dữ liệu: " + e.getMessage(), "error");
        }
    }

    private void promptForMissingRecoveryPinIfNeeded() {
        if (currentUser == null || currentUser.hasRecoveryPin()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("Thiết lập bảo mật");
        alert.setHeaderText("Tài khoản này chưa có mã PIN khôi phục");
        alert.setContentText("Bạn nên thiết lập mã PIN 6 số ngay bây giờ để có thể khôi phục mật khẩu khi cần.");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        ButtonType setPinButton = new ButtonType("Thiết lập ngay", ButtonBar.ButtonData.OK_DONE);
        ButtonType laterButton = new ButtonType("Để sau", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(setPinButton, laterButton);

        if (alert.showAndWait().orElse(laterButton) == setPinButton) {
            showRecoveryPinDialogClean();
        }
    }

    private void performLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText("Bạn muốn đăng xuất khỏi tài khoản hiện tại?");
        confirm.setContentText("Phiên làm việc hiện tại sẽ đóng và quay về màn hình đăng nhập.");
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK && onLogout != null) {
            saveWindowSettings();
            stage.close();
            onLogout.run();
        }
    }

    public void showToast(String message, String type) {
        Platform.runLater(() -> new ToastNotification(message, type).show(stage));
    }

    private void setupReminderCallbacks() {
        reminderService.setNotificationCallback(this::onReminderTriggered);
        reminderService.setBadgeCallback(count -> {
            if (badgeLabel != null) {
                badgeLabel.setText(count > 9 ? "9+" : String.valueOf(count));
                badgeLabel.setVisible(count > 0);
            }
        });
    }

    private void onReminderTriggered(Reminder reminder) {
        showToast("Nhắc nhở: " + reminder.getTaskTitle(), "warning");
    }

    public void refreshSidebar() {
        sidebar = buildSidebar();
        root.setLeft(sidebar);
    }

    public void refreshAllDataViews() {
        refreshSidebar();
        if (dashboardView != null) {
            dashboardView.refresh();
        }
        if (taskListView != null) {
            taskListView.refresh();
        }
        if (calendarView != null) {
            calendarView.refresh();
        }
        if (chartView != null) {
            chartView.refresh();
        }
    }

    private void applyWindowSettings() {
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT `key`, value FROM settings WHERE `key` IN ('window_width','window_height')")) {
            while (rs.next()) {
                switch (rs.getString("key")) {
                    case "window_width" -> stage.setWidth(Double.parseDouble(rs.getString("value")));
                    case "window_height" -> stage.setHeight(Double.parseDouble(rs.getString("value")));
                    default -> {
                    }
                }
            }
        } catch (Exception e) {
            stage.setWidth(1200);
            stage.setHeight(750);
        }
        stage.centerOnScreen();
    }

    private void saveWindowSettings() {
        try {
            var conn = DBConnection.getConnection();
            var ps = conn.prepareStatement("REPLACE INTO settings (`key`, value) VALUES (?, ?)");
            ps.setString(1, "window_width");
            ps.setString(2, String.valueOf((int) stage.getWidth()));
            ps.executeUpdate();
            ps.setString(1, "window_height");
            ps.setString(2, String.valueOf((int) stage.getHeight()));
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private String safeText(String value) {
        return value != null && !value.isBlank() ? value : "Chưa có";
    }

    private String formatLastLogin(LocalDateTime lastLoginAt) {
        return lastLoginAt != null ? lastLoginAt.format(LAST_LOGIN_FORMAT) : "Chưa ghi nhận";
    }

    private String normalizeTime(String rawTime) {
        String value = rawTime == null ? "" : rawTime.trim();
        if (value.matches("\\d{1,2}:\\d{2}")) {
            String[] parts = value.split(":");
            return String.format("%02d:%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        throw new IllegalArgumentException("Invalid time");
    }

    private Reminder.RepeatType parseRepeatType(String value) {
        return switch (Objects.requireNonNullElse(value, "Không lặp")) {
            case "Hàng ngày" -> Reminder.RepeatType.DAILY;
            case "Hàng tuần" -> Reminder.RepeatType.WEEKLY;
            default -> Reminder.RepeatType.NONE;
        };
    }

    private String valueOf(TextField field) {
        return field.getText() != null ? field.getText() : "";
    }

    public Stage getStage() {
        return stage;
    }

    public ReminderService getReminderService() {
        return reminderService;
    }

    public int getCurrentFolderId() {
        return currentFolderId;
    }
}
