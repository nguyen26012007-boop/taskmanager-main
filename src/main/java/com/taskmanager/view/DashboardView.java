package com.taskmanager.view;

import com.taskmanager.controller.TaskController;
import com.taskmanager.model.Task;
import com.taskmanager.util.AnimationUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Dashboard tổng quan hiển thị thống kê, charts và task hôm nay.
 */
public class DashboardView {

    private final MainWindow mainWindow;
    private final ScrollPane root;
    private final VBox contentBox;

    private Label totalLabel;
    private Label inProgressLabel;
    private Label doneLabel;
    private Label overdueLabel;
    private PieChart donutChart;
    private BarChart<String, Number> barChart;
    private VBox todayTaskList;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    public DashboardView(MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        contentBox = new VBox(20);
        contentBox.setPadding(new Insets(24));
        contentBox.getStyleClass().add("dashboard-content");

        root = new ScrollPane(contentBox);
        root.setFitToWidth(true);
        root.getStyleClass().add("transparent-scroll");
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        buildUI();
        refresh();
    }

    private void buildUI() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Tổng quan");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Chào buổi sáng! Hôm nay bạn có việc cần làm.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox headerText = new VBox(4, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button();
        FontIcon refreshIcon = new FontIcon(FontAwesomeSolid.SYNC_ALT);
        refreshIcon.setIconSize(14);
        refreshBtn.setGraphic(refreshIcon);
        refreshBtn.getStyleClass().add("icon-button");
        refreshBtn.setOnAction(e -> refresh());

        header.getChildren().addAll(headerText, spacer, refreshBtn);

        HBox statsRow = buildStatCards();
        HBox chartsRow = buildChartsRow();
        VBox todaySection = buildTodaySection();

        contentBox.getChildren().addAll(header, statsRow, chartsRow, todaySection);
    }

    private HBox buildStatCards() {
        HBox row = new HBox(16);

        String[][] cardData = {
            {"Tổng công việc", "0", "#2F6FED"},
            {"Đang thực hiện", "0", "#E94560"},
            {"Hoàn thành", "0", "#00C896"},
            {"Quá hạn", "0", "#FFB800"}
        };

        FontAwesomeSolid[] icons = {
            FontAwesomeSolid.LIST_UL,
            FontAwesomeSolid.SPINNER,
            FontAwesomeSolid.CHECK_CIRCLE,
            FontAwesomeSolid.EXCLAMATION_TRIANGLE
        };

        for (int i = 0; i < cardData.length; i++) {
            VBox card = buildStatCard(cardData[i][0], cardData[i][1], cardData[i][2], icons[i]);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);

            Label valueLabel = (Label) ((VBox) card.getChildren().get(1)).getChildren().get(0);
            switch (i) {
                case 0 -> totalLabel = valueLabel;
                case 1 -> inProgressLabel = valueLabel;
                case 2 -> doneLabel = valueLabel;
                case 3 -> overdueLabel = valueLabel;
            }
        }

        return row;
    }

    private VBox buildStatCard(String title, String value, String accentColor, FontAwesomeSolid icon) {
        VBox card = new VBox(12);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(20));
        card.setMinHeight(120);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("stat-icon-box");
        iconBox.setPrefSize(44, 44);
        iconBox.setStyle("-fx-background-color: " + accentColor + "22; -fx-background-radius: 12;");
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(20);
        fontIcon.setIconColor(Color.web(accentColor));
        iconBox.getChildren().add(fontIcon);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        valueLabel.setStyle("-fx-text-fill: " + accentColor + ";");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        VBox textBox = new VBox(4, valueLabel, titleLabel);

        card.getChildren().addAll(iconBox, textBox);
        AnimationUtil.addHoverEffect(card, accentColor);
        return card;
    }

    private HBox buildChartsRow() {
        HBox row = new HBox(16);

        VBox donutBox = buildDonutChartCard();
        HBox.setHgrow(donutBox, Priority.ALWAYS);

        VBox barBox = buildBarChartCard();
        HBox.setHgrow(barBox, Priority.ALWAYS);

        row.getChildren().addAll(donutBox, barBox);
        return row;
    }

    private VBox buildDonutChartCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("chart-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Tỷ lệ hoàn thành");
        title.getStyleClass().add("card-title");

        donutChart = new PieChart();
        donutChart.getStyleClass().add("donut-chart");
        donutChart.setLabelsVisible(false);
        donutChart.setLegendVisible(true);
        donutChart.setPrefHeight(220);
        donutChart.setAnimated(true);

        card.getChildren().addAll(title, donutChart);
        return card;
    }

    private VBox buildBarChartCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("chart-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Hoàn thành 7 ngày qua");
        title.getStyleClass().add("card-title");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Số task");

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.getStyleClass().add("dark-bar-chart");
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setPrefHeight(220);
        barChart.setBarGap(4);
        barChart.setCategoryGap(12);

        card.getChildren().addAll(title, barChart);
        return card;
    }

    private VBox buildTodaySection() {
        VBox section = new VBox(12);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Việc cần làm hôm nay");
        title.getStyleClass().add("section-title");

        Label dueSoonBadge = new Label("⚠ Sắp đến hạn");
        dueSoonBadge.getStyleClass().add("due-soon-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button viewAllBtn = new Button("Xem tất cả →");
        viewAllBtn.getStyleClass().add("link-button");
        viewAllBtn.setOnAction(e -> mainWindow.navigateTo("tasks"));

        header.getChildren().addAll(title, dueSoonBadge, spacer, viewAllBtn);

        todayTaskList = new VBox(8);
        todayTaskList.getStyleClass().add("today-task-list");

        section.getChildren().addAll(header, todayTaskList);
        return section;
    }

    public void refresh() {
        javafx.concurrent.Task<int[]> statsTask = new javafx.concurrent.Task<>() {
            @Override
            protected int[] call() throws Exception {
                return new TaskController().getStatistics();
            }
        };

        statsTask.setOnSucceeded(e -> updateStatistics(statsTask.getValue()));
        statsTask.setOnFailed(e -> System.err.println("Lỗi load stats: " + statsTask.getException()));

        javafx.concurrent.Task<List<Task>> todayTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Task> call() throws Exception {
                return new TaskController().getAllTasks(2, null, null);
            }
        };

        todayTask.setOnSucceeded(e -> updateTodayTaskList(todayTask.getValue()));

        javafx.concurrent.Task<Map<LocalDate, Long>> weeklyStatsTask = new javafx.concurrent.Task<>() {
            @Override
            protected Map<LocalDate, Long> call() throws Exception {
                return new TaskController().getCompletionCountsLastDays(7);
            }
        };

        weeklyStatsTask.setOnSucceeded(e -> updateBarChart(weeklyStatsTask.getValue()));

        new Thread(statsTask).start();
        new Thread(todayTask).start();
        new Thread(weeklyStatsTask).start();
    }

    private void updateStatistics(int[] stats) {
        if (totalLabel != null) AnimationUtil.animateNumber(totalLabel, 0, stats[0], 800);
        if (inProgressLabel != null) AnimationUtil.animateNumber(inProgressLabel, 0, stats[2], 800);
        if (doneLabel != null) AnimationUtil.animateNumber(doneLabel, 0, stats[3], 800);
        if (overdueLabel != null) AnimationUtil.animateNumber(overdueLabel, 0, stats[4], 800);

        int total = stats[0];
        int done = stats[3];
        int remaining = Math.max(total - done, 0);

        donutChart.getData().clear();
        if (total <= 0) {
            return;
        }

        PieChart.Data completedData = new PieChart.Data("Hoàn thành (" + done + ")", done);
        PieChart.Data remainingData = new PieChart.Data("Còn lại (" + remaining + ")", remaining);
        donutChart.getData().addAll(completedData, remainingData);

    }

    private void updateTodayTaskList(List<Task> tasks) {
        todayTaskList.getChildren().clear();
        List<Task> limited = tasks.stream().limit(5).toList();

        if (limited.isEmpty()) {
            Label emptyLabel = new Label("Tuyệt vời! Không có task nào hôm nay.");
            emptyLabel.getStyleClass().add("empty-state-label");
            todayTaskList.getChildren().add(emptyLabel);
            return;
        }

        for (Task task : limited) {
            todayTaskList.getChildren().add(buildTodayTaskRow(task));
        }

        AnimationUtil.staggerFadeIn(todayTaskList.getChildren().stream().toList(), 80);
    }

    private HBox buildTodayTaskRow(Task task) {
        HBox row = new HBox(12);
        row.getStyleClass().add("today-task-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(task.getStatus() == Task.Status.DONE);
        checkBox.getStyleClass().add("task-checkbox");
        checkBox.setOnAction(e -> {
            Task.Status newStatus = checkBox.isSelected() ? Task.Status.DONE : Task.Status.TODO;
            try {
                new TaskController().updateStatus(task.getId(), newStatus);
                refresh();
            } catch (Exception ex) {
                mainWindow.showToast("Lỗi cập nhật: " + ex.getMessage(), "error");
            }
        });

        Circle priorityDot = new Circle(5);
        priorityDot.setFill(Color.web(task.getPriorityColor()));

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("today-task-title");
        if (task.getStatus() == Task.Status.DONE) {
            titleLabel.getStyleClass().add("task-done");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (task.isDueSoon() || task.isOverdue()) {
            Label dueLabel = new Label(task.isOverdue()
                ? "Quá hạn!"
                : (task.getDueDate() != null ? task.getDueDate().format(TIME_FORMAT) : ""));
            dueLabel.getStyleClass().add(task.isOverdue() ? "overdue-badge" : "due-badge");
            row.getChildren().addAll(checkBox, priorityDot, titleLabel, spacer, dueLabel);
        } else {
            row.getChildren().addAll(checkBox, priorityDot, titleLabel, spacer);
        }

        if (!task.getSubtasks().isEmpty()) {
            ProgressBar subProgress = new ProgressBar(task.getCompletionPercent() / 100);
            subProgress.getStyleClass().add("task-progress-bar");
            subProgress.setPrefWidth(60);
            row.getChildren().add(subProgress);
        }

        row.setOnMouseClicked(e -> mainWindow.showAddTaskDialog(task));
        AnimationUtil.addHoverEffect(row, task.getPriorityColor());
        return row;
    }

    private void updateBarChart(Map<LocalDate, Long> completionCounts) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Task hoàn thành");

        completionCounts.forEach((date, count) ->
            series.getData().add(new XYChart.Data<>(toDayLabel(date), count)));

        // Optimize Y-axis to show only clean integer values
        long maxCount = completionCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        NumberAxis yAxis = (NumberAxis) barChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);

        long upperBound = Math.max(5, ((maxCount + 4) / 5) * 5); // Round up to nearest multiple of 5, minimum 5
        long tickUnit = upperBound / 5;
        if (tickUnit == 0) tickUnit = 1;

        yAxis.setUpperBound(upperBound);
        yAxis.setTickUnit(tickUnit);
        yAxis.setMinorTickVisible(false);

        barChart.getData().clear();
        barChart.getData().add(series);
    }

    private String toDayLabel(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    public ScrollPane getRoot() {
        return root;
    }
}
