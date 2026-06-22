package com.taskmanager.view;

import com.taskmanager.controller.TaskController;
import com.taskmanager.model.Task;
import com.taskmanager.util.AnimationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CalendarView {

    private static final String[] DAY_NAMES = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"};
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("vi", "VN"));
    private static final DateTimeFormatter DAY_MONTH_FORMAT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FULL_DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));

    private final MainWindow mainWindow;
    private final BorderPane root;

    private LocalDate currentWeekStart;
    private LocalDate selectedDate;
    private GridPane calendarGrid;
    private Label monthLabel;
    private Label weekRangeLabel;
    private VBox selectedDayPanel;

    private List<Task> allTasks = List.of();
    private Map<LocalDate, List<Task>> tasksByDate = Map.of();

    public CalendarView(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.selectedDate = LocalDate.now();
        this.currentWeekStart = getWeekStart(selectedDate);
        this.root = new BorderPane();
        this.root.getStyleClass().add("calendar-root");
        buildUI();
    }

    private void buildUI() {
        VBox topSection = new VBox(10);
        topSection.getChildren().addAll(buildToolbar(), buildHelperBar());
        root.setTop(topSection);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");

        calendarGrid = new GridPane();
        calendarGrid.getStyleClass().add("calendar-grid");
        calendarGrid.setPadding(new Insets(0, 20, 20, 20));

        scrollPane.setContent(calendarGrid);
        root.setCenter(scrollPane);

        selectedDayPanel = new VBox(12);
        selectedDayPanel.getStyleClass().add("calendar-side-panel");
        selectedDayPanel.setPadding(new Insets(16));
        selectedDayPanel.setPrefWidth(310);
        root.setRight(selectedDayPanel);

        renderWeek();
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(16, 24, 4, 24));
        toolbar.getStyleClass().add("toolbar");

        VBox titleBox = new VBox(4);
        Label title = new Label("Lịch biểu");
        title.getStyleClass().add("page-title");

        weekRangeLabel = new Label();
        weekRangeLabel.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, weekRangeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button prevButton = new Button();
        FontIcon prevIcon = new FontIcon(FontAwesomeSolid.CHEVRON_LEFT);
        prevIcon.setIconSize(13);
        prevButton.setGraphic(prevIcon);
        prevButton.getStyleClass().add("nav-arrow-btn");
        prevButton.setOnAction(e -> {
            currentWeekStart = currentWeekStart.minusWeeks(1);
            selectedDate = currentWeekStart;
            renderWeek();
        });

        Button todayButton = new Button("Hôm nay");
        todayButton.getStyleClass().add("today-btn");
        todayButton.setOnAction(e -> {
            selectedDate = LocalDate.now();
            currentWeekStart = getWeekStart(selectedDate);
            renderWeek();
        });

        Button nextButton = new Button();
        FontIcon nextIcon = new FontIcon(FontAwesomeSolid.CHEVRON_RIGHT);
        nextIcon.setIconSize(13);
        nextButton.setGraphic(nextIcon);
        nextButton.getStyleClass().add("nav-arrow-btn");
        nextButton.setOnAction(e -> {
            currentWeekStart = currentWeekStart.plusWeeks(1);
            selectedDate = currentWeekStart;
            renderWeek();
        });

        monthLabel = new Label();
        monthLabel.getStyleClass().add("month-label");

        toolbar.getChildren().addAll(titleBox, spacer, prevButton, todayButton, nextButton, monthLabel);
        return toolbar;
    }

    private HBox buildHelperBar() {
        HBox helperBar = new HBox(16);
        helperBar.setAlignment(Pos.CENTER_LEFT);
        helperBar.setPadding(new Insets(0, 24, 12, 24));
        helperBar.getStyleClass().add("calendar-helper-bar");

        Label hint = new Label("Bấm một lần để chọn ngày. Nháy đúp vào tiêu đề ngày hoặc ô giờ để thêm task cho đúng ngày.");
        hint.getStyleClass().add("calendar-helper-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        helperBar.getChildren().addAll(
            hint,
            spacer,
            buildLegendLabel("#E94560", "Ưu tiên cao"),
            buildLegendLabel("#FFB800", "Ưu tiên trung bình"),
            buildLegendLabel("#00C896", "Ưu tiên thấp")
        );
        return helperBar;
    }

    private Label buildLegendLabel(String color, String text) {
        Label label = new Label("● " + text);
        label.getStyleClass().add("calendar-legend");
        label.setStyle("-fx-text-fill: " + color + ";");
        return label;
    }

    private void renderWeek() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        if (selectedDate.isBefore(currentWeekStart) || selectedDate.isAfter(currentWeekStart.plusDays(6))) {
            selectedDate = currentWeekStart;
        }
        updateDateLabels();
        loadTasksForWeek();
    }

    private void updateDateLabels() {
        LocalDate weekEnd = currentWeekStart.plusDays(6);
        monthLabel.setText(currentWeekStart.format(MONTH_FORMAT));
        weekRangeLabel.setText("Tuần " + currentWeekStart.format(DAY_MONTH_FORMAT) + " - " + weekEnd.format(DAY_MONTH_FORMAT));
    }

    private void loadTasksForWeek() {
        javafx.concurrent.Task<List<Task>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Task> call() throws Exception {
                return new TaskController().getAllTasks(null, null, null);
            }
        };

        loadTask.setOnSucceeded(e -> {
            allTasks = loadTask.getValue();
            tasksByDate = allTasks.stream()
                .filter(task -> task.getDueDate() != null)
                .collect(Collectors.groupingBy(Task::getDueDate));
            buildCalendarGrid();
            buildSelectedDayPanel();
        });

        loadTask.setOnFailed(e -> mainWindow.showToast(
            "Không thể tải dữ liệu lịch: " + loadTask.getException().getMessage(),
            "error"
        ));

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void buildCalendarGrid() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.setHgap(1);
        calendarGrid.setVgap(0);

        ColumnConstraints timeColumn = new ColumnConstraints(72);
        calendarGrid.getColumnConstraints().add(timeColumn);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints dayColumn = new ColumnConstraints();
            dayColumn.setHgrow(Priority.ALWAYS);
            dayColumn.setMinWidth(128);
            calendarGrid.getColumnConstraints().add(dayColumn);
        }

        buildDayHeaders();
        buildSummaryRow();
        for (int hour = 0; hour < 24; hour++) {
            buildTimeRow(hour);
        }

        AnimationUtil.fadeIn(calendarGrid, 250);
    }

    private void buildDayHeaders() {
        StackPane cornerCell = new StackPane();
        cornerCell.getStyleClass().add("calendar-corner");
        calendarGrid.add(cornerCell, 0, 0);

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate day = currentWeekStart.plusDays(i);
            boolean isToday = day.equals(today);
            boolean isSelected = day.equals(selectedDate);
            List<Task> dayTasks = getTasksForDate(day);

            VBox header = new VBox(4);
            header.setAlignment(Pos.CENTER);
            header.setPadding(new Insets(10, 8, 10, 8));
            header.getStyleClass().add(isToday ? "day-header-today" : "day-header");
            if (isSelected) {
                header.getStyleClass().add("day-header-selected");
            }

            Label dayName = new Label(DAY_NAMES[i]);
            dayName.getStyleClass().add(isToday ? "day-name-today" : "day-name");

            StackPane dateCircle = new StackPane();
            Label dateNumber = new Label(String.valueOf(day.getDayOfMonth()));
            if (isToday) {
                Circle circle = new Circle(16);
                circle.setFill(Color.web("#E94560"));
                dateNumber.getStyleClass().add("date-number-today");
                dateCircle.getChildren().addAll(circle, dateNumber);
            } else {
                dateNumber.getStyleClass().add("date-number");
                dateCircle.getChildren().add(dateNumber);
            }

            Label countLabel = new Label(dayTasks.isEmpty() ? "Chưa có task" : dayTasks.size() + " task");
            countLabel.getStyleClass().add("calendar-day-count");

            header.getChildren().addAll(dayName, dateCircle, countLabel);
            header.setOnMouseClicked(e -> {
                selectedDate = day;
                if (e.getClickCount() == 2) {
                    openNewTaskForDate(day);
                } else {
                    renderWeek();
                }
            });

            calendarGrid.add(header, i + 1, 0);
        }
    }

    private void buildSummaryRow() {
        Label summaryLabel = new Label("Trong ngày");
        summaryLabel.getStyleClass().add("time-label");
        summaryLabel.setPadding(new Insets(10, 8, 10, 4));
        calendarGrid.add(summaryLabel, 0, 1);

        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            LocalDate day = currentWeekStart.plusDays(dayIndex);
            boolean isSelected = day.equals(selectedDate);

            VBox summaryCell = new VBox(6);
            summaryCell.getStyleClass().add("calendar-summary-cell");
            if (isSelected) {
                summaryCell.getStyleClass().add("calendar-summary-cell-selected");
            }
            summaryCell.setPadding(new Insets(10));
            summaryCell.setMinHeight(132);
            summaryCell.setOnMouseClicked(e -> {
                selectedDate = day;
                if (e.getClickCount() == 2) {
                    openNewTaskForDate(day);
                } else {
                    renderWeek();
                }
            });

            List<Task> dayTasks = getTasksForDate(day);
            if (dayTasks.isEmpty()) {
                Label emptyLabel = new Label("Chưa có công việc");
                emptyLabel.getStyleClass().add("calendar-summary-empty");
                Hyperlink addLink = new Hyperlink("+ Thêm task");
                addLink.getStyleClass().add("calendar-add-link");
                addLink.setOnAction(e -> openNewTaskForDate(day));
                summaryCell.getChildren().addAll(emptyLabel, addLink);
            } else {
                int displayCount = Math.min(dayTasks.size(), 4);
                for (int i = 0; i < displayCount; i++) {
                    summaryCell.getChildren().add(buildTaskChip(dayTasks.get(i)));
                }
                if (dayTasks.size() > displayCount) {
                    Label moreLabel = new Label("+" + (dayTasks.size() - displayCount) + " task nữa");
                    moreLabel.getStyleClass().add("more-tasks-label");
                    summaryCell.getChildren().add(moreLabel);
                }
                Hyperlink addLink = new Hyperlink("+ Thêm task trong ngày");
                addLink.getStyleClass().add("calendar-add-link");
                addLink.setOnAction(e -> openNewTaskForDate(day));
                summaryCell.getChildren().add(addLink);
            }

            calendarGrid.add(summaryCell, dayIndex + 1, 1);
        }
    }

    private void buildTimeRow(int hour) {
        int gridRow = hour + 2;

        Label timeLabel = new Label(String.format("%02d:00", hour));
        timeLabel.getStyleClass().add("time-label");
        timeLabel.setPadding(new Insets(4, 8, 4, 4));
        calendarGrid.add(timeLabel, 0, gridRow);

        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            LocalDate date = currentWeekStart.plusDays(dayIndex);
            calendarGrid.add(buildTimeCell(date, hour), dayIndex + 1, gridRow);
        }
    }

    private StackPane buildTimeCell(LocalDate date, int hour) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("time-cell");
        cell.setMinHeight(42);
        cell.setPrefHeight(42);

        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("time-cell-today");
        }
        if (date.equals(selectedDate)) {
            cell.getStyleClass().add("time-cell-selected");
        }

        if (hour == 9) {
            Label hint = new Label("Nháy đúp để thêm");
            hint.getStyleClass().add("calendar-cell-hint");
            cell.getChildren().add(hint);
        }

        Tooltip.install(cell, new Tooltip("Tạo task cho ngày " + date.format(DAY_MONTH_FORMAT)));
        cell.setOnMouseClicked(e -> {
            selectedDate = date;
            if (e.getClickCount() == 2) {
                openNewTaskForDate(date);
            } else {
                renderWeek();
            }
        });
        cell.setOnMouseEntered(e -> {
            if (!cell.getStyleClass().contains("time-cell-hover")) {
                cell.getStyleClass().add("time-cell-hover");
            }
        });
        cell.setOnMouseExited(e -> cell.getStyleClass().remove("time-cell-hover"));

        return cell;
    }

    private HBox buildTaskChip(Task task) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(5, 8, 5, 8));
        chip.getStyleClass().add("calendar-task-chip");
        chip.setStyle(
            "-fx-background-color: " + task.getPriorityColor() + "22; " +
            "-fx-border-color: " + task.getPriorityColor() + "; " +
            "-fx-background-radius: 8; -fx-border-radius: 8;"
        );

        Circle dot = new Circle(3.5, Color.web(task.getPriorityColor()));

        VBox textBox = new VBox(2);
        Label title = new Label(task.getTitle());
        title.getStyleClass().add("chip-title");
        title.setWrapText(true);

        Label meta = new Label(buildTaskMeta(task));
        meta.getStyleClass().add("calendar-chip-meta");
        textBox.getChildren().addAll(title, meta);

        chip.getChildren().addAll(dot, textBox);
        chip.setOnMouseClicked(e -> {
            e.consume();
            mainWindow.showAddTaskDialog(task);
        });
        return chip;
    }

    private void buildSelectedDayPanel() {
        selectedDayPanel.getChildren().clear();

        Label title = new Label("Ngày đang chọn");
        title.getStyleClass().add("section-title");

        Label dateLabel = new Label(selectedDate.format(FULL_DAY_FORMAT));
        dateLabel.getStyleClass().add("page-subtitle");

        List<Task> selectedTasks = getTasksForDate(selectedDate);
        Label countLabel = new Label(selectedTasks.isEmpty()
            ? "Không có công việc nào trong ngày này."
            : "Có " + selectedTasks.size() + " công việc trong ngày.");
        countLabel.getStyleClass().add("calendar-helper-text");

        Button addButton = new Button("+ Thêm task cho ngày này");
        addButton.getStyleClass().add("primary-button");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> openNewTaskForDate(selectedDate));

        VBox listBox = new VBox(10);
        listBox.setFillWidth(true);
        if (selectedTasks.isEmpty()) {
            Label empty = new Label("Chưa có task được lên lịch cho ngày này.");
            empty.getStyleClass().add("calendar-summary-empty");
            listBox.getChildren().add(empty);
        } else {
            for (Task task : selectedTasks) {
                listBox.getChildren().add(buildSelectedDayTaskCard(task));
            }
        }

        selectedDayPanel.getChildren().addAll(title, dateLabel, countLabel, addButton, listBox);
    }

    private VBox buildSelectedDayTaskCard(Task task) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(12));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5, Color.web(task.getPriorityColor()));
        Label title = new Label(task.getTitle());
        title.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        header.getChildren().addAll(dot, title);

        Label status = new Label("Trạng thái: " + task.getStatusText());
        status.getStyleClass().add("page-subtitle");

        Label folder = new Label("Thư mục: " + (task.getFolderName() != null && !task.getFolderName().isBlank()
            ? task.getFolderName()
            : "Chưa phân loại"));
        folder.getStyleClass().add("page-subtitle");

        Hyperlink editLink = new Hyperlink("Mở để chỉnh sửa");
        editLink.getStyleClass().add("calendar-add-link");
        editLink.setOnAction(e -> mainWindow.showAddTaskDialog(task));

        card.getChildren().addAll(header, status, folder, editLink);
        return card;
    }

    private String buildTaskMeta(Task task) {
        String folder = task.getFolderName() != null && !task.getFolderName().isBlank() ? task.getFolderName() : "Chưa phân loại";
        return task.getStatusText() + " • " + folder;
    }

    private List<Task> getTasksForDate(LocalDate date) {
        return tasksByDate.getOrDefault(date, List.of()).stream()
            .sorted(Comparator
                .comparing((Task task) -> task.getStatus() == Task.Status.DONE)
                .thenComparing(task -> task.getPriority().ordinal())
                .thenComparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    private void openNewTaskForDate(LocalDate date) {
        Task newTask = new Task();
        newTask.setDueDate(date);
        mainWindow.showAddTaskDialog(newTask);
    }

    private LocalDate getWeekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    public void refresh() {
        renderWeek();
    }

    public BorderPane getRoot() {
        return root;
    }
}
