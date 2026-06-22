package com.taskmanager.view;

import com.taskmanager.controller.TaskController;
import com.taskmanager.model.Task;
import com.taskmanager.util.AnimationUtil;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ChartView {

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM");

    private final MainWindow mainWindow;
    private final BorderPane root;
    private final ScrollPane scrollPane;

    private List<Task> allTasks = List.of();
    private ToggleGroup periodGroup;
    private VBox chartsContainer;
    private PieChart donutChart;
    private BarChart<String, Number> barChart;
    private LineChart<String, Number> lineChart;
    private Label completionValueLabel;
    private Label productivityValueLabel;
    private Label onTimeValueLabel;
    private Label streakValueLabel;

    public ChartView(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.root = new BorderPane();
        this.root.getStyleClass().add("chart-view-root");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));

        this.scrollPane = new ScrollPane(content);
        this.scrollPane.setFitToWidth(true);
        this.scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.scrollPane.getStyleClass().add("transparent-scroll");

        buildUI(content);
        this.root.setCenter(scrollPane);
    }

    private void buildUI(VBox content) {
        HBox header = buildHeader();
        HBox filterBar = buildFilterBar();
        HBox summaryCards = buildSummaryCards();

        chartsContainer = new VBox(20);
        chartsContainer.getChildren().addAll(buildTopCharts(), buildLineChartCard());

        content.getChildren().addAll(header, filterBar, summaryCards, chartsContainer);
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Thong ke & Bieu do");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Phan tich hieu suat lam viec cua ban");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportBtn = new Button("Xuat bieu do");
        exportBtn.getStyleClass().add("toolbar-button");
        FontIcon exportIcon = new FontIcon(FontAwesomeSolid.FILE_IMAGE);
        exportIcon.setIconSize(13);
        exportBtn.setGraphic(exportIcon);
        exportBtn.setOnAction(event -> exportChartAsPNG());

        header.getChildren().addAll(titleBox, spacer, exportBtn);
        return header;
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Khoang thoi gian:");
        filterLabel.getStyleClass().add("form-label");

        periodGroup = new ToggleGroup();
        String[] periods = {"7 ngay", "30 ngay", "3 thang", "Tat ca"};
        for (int i = 0; i < periods.length; i++) {
            ToggleButton button = new ToggleButton(periods[i]);
            button.setToggleGroup(periodGroup);
            button.getStyleClass().add("period-toggle");
            button.setUserData(periods[i]);
            if (i == 0) {
                button.setSelected(true);
            }
            button.setOnAction(event -> {
                if (button.isSelected()) {
                    applyPeriodFilter((String) button.getUserData());
                }
            });
            bar.getChildren().add(button);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        DatePicker fromDate = new DatePicker(LocalDate.now().minusDays(7));
        fromDate.getStyleClass().add("form-datepicker");
        fromDate.setPrefWidth(130);

        Label toLabel = new Label("den");
        toLabel.getStyleClass().add("form-sub-label");

        DatePicker toDate = new DatePicker(LocalDate.now());
        toDate.getStyleClass().add("form-datepicker");
        toDate.setPrefWidth(130);

        Button applyBtn = new Button("Ap dung");
        applyBtn.getStyleClass().add("secondary-button");
        applyBtn.setOnAction(event -> applyCustomFilter(fromDate.getValue(), toDate.getValue()));

        bar.getChildren().addAll(filterLabel, spacer, fromDate, toLabel, toDate, applyBtn);
        return bar;
    }

    private HBox buildSummaryCards() {
        HBox row = new HBox(16);

        VBox completionCard = buildMiniStatCard("Ty le hoan thanh", "#00C896", FontAwesomeSolid.PERCENTAGE);
        VBox productivityCard = buildMiniStatCard("Task/ngay TB", "#E94560", FontAwesomeSolid.BOLT);
        VBox onTimeCard = buildMiniStatCard("Dung han", "#FFB800", FontAwesomeSolid.CLOCK);
        VBox streakCard = buildMiniStatCard("Chuoi ngay", "#0F3460", FontAwesomeSolid.FIRE);

        completionValueLabel = (Label) completionCard.getChildren().get(1);
        productivityValueLabel = (Label) productivityCard.getChildren().get(1);
        onTimeValueLabel = (Label) onTimeCard.getChildren().get(1);
        streakValueLabel = (Label) streakCard.getChildren().get(1);

        for (VBox card : List.of(completionCard, productivityCard, onTimeCard, streakCard)) {
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private VBox buildMiniStatCard(String title, String color, FontAwesomeSolid icon) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(16));

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(32, 32);
        iconBox.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 8;");

        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(15);
        fontIcon.setIconColor(Color.web(color));
        iconBox.getChildren().add(fontIcon);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        topRow.getChildren().addAll(iconBox, titleLabel);

        Label valueLabel = new Label("--");
        valueLabel.getStyleClass().add("stat-value");
        valueLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22;");

        card.getChildren().addAll(topRow, valueLabel);
        AnimationUtil.addHoverEffect(card, color);
        return card;
    }

    private HBox buildTopCharts() {
        HBox row = new HBox(16);

        VBox donutCard = buildDonutCard();
        VBox barCard = buildBarCard();
        HBox.setHgrow(donutCard, Priority.ALWAYS);
        HBox.setHgrow(barCard, Priority.ALWAYS);

        row.getChildren().addAll(donutCard, barCard);
        return row;
    }

    private VBox buildDonutCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("chart-card");
        card.setPadding(new Insets(20));

        HBox cardHeader = new HBox();
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Phan bo theo trang thai");
        title.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        cardHeader.getChildren().addAll(title, spacer);

        donutChart = new PieChart();
        donutChart.getStyleClass().add("donut-chart");
        donutChart.setLabelsVisible(true);
        donutChart.setLegendVisible(true);
        donutChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        donutChart.setPrefHeight(280);
        donutChart.setAnimated(true);

        HBox legend = buildChartLegend(
            new String[]{"Chua bat dau", "Dang lam", "Hoan thanh", "Tam hoan"},
            new String[]{"#A0A0B0", "#E94560", "#00C896", "#FFB800"}
        );

        card.getChildren().addAll(cardHeader, donutChart, legend);
        return card;
    }

    private VBox buildBarCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("chart-card");
        card.setPadding(new Insets(20));

        HBox cardHeader = new HBox();
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Task hoan thanh theo ngay");
        title.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleGroup toggleGroup = new ToggleGroup();
        ToggleButton weekButton = new ToggleButton("Tuan");
        ToggleButton monthButton = new ToggleButton("Thang");
        weekButton.setToggleGroup(toggleGroup);
        monthButton.setToggleGroup(toggleGroup);
        weekButton.setSelected(true);
        weekButton.getStyleClass().add("mini-toggle");
        monthButton.getStyleClass().add("mini-toggle");
        HBox toggleRow = new HBox(0, weekButton, monthButton);
        toggleRow.getStyleClass().add("mini-toggle-group");

        cardHeader.getChildren().addAll(title, spacer, toggleRow);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("So task");

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.getStyleClass().add("dark-bar-chart");
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setPrefHeight(280);
        barChart.setBarGap(3);
        barChart.setCategoryGap(10);

        weekButton.setOnAction(event -> updateBarChart(allTasks, "7 ngay"));
        monthButton.setOnAction(event -> updateBarChart(allTasks, "30 ngay"));

        card.getChildren().addAll(cardHeader, barChart);
        return card;
    }

    private VBox buildLineChartCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("chart-card");
        card.setPadding(new Insets(20));

        HBox cardHeader = new HBox();
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Xu huong hieu suat lam viec");
        title.getStyleClass().add("card-title");
        Label subtitle = new Label("Task tao moi va task hoan thanh theo thoi gian");
        subtitle.getStyleClass().add("page-subtitle");
        VBox titleBox = new VBox(2, title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        cardHeader.getChildren().addAll(titleBox, spacer);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.getStyleClass().add("dark-line-chart");
        lineChart.setAnimated(true);
        lineChart.setPrefHeight(250);
        lineChart.setCreateSymbols(true);

        HBox legend = buildChartLegend(
            new String[]{"Task tao moi", "Task hoan thanh"},
            new String[]{"#E94560", "#00C896"}
        );

        card.getChildren().addAll(cardHeader, lineChart, legend);
        return card;
    }

    private HBox buildChartLegend(String[] labels, String[] colors) {
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER);
        for (int i = 0; i < labels.length; i++) {
            HBox item = new HBox(6);
            item.setAlignment(Pos.CENTER_LEFT);
            Rectangle dot = new Rectangle(10, 10);
            dot.setFill(Color.web(colors[i]));
            dot.setArcWidth(3);
            dot.setArcHeight(3);

            Label label = new Label(labels[i]);
            label.getStyleClass().add("legend-label");
            item.getChildren().addAll(dot, label);
            legend.getChildren().add(item);
        }
        return legend;
    }

    public void refresh() {
        javafx.concurrent.Task<List<Task>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Task> call() throws Exception {
                return new TaskController().getAllTasks(com.taskmanager.dao.FolderDAO.ALL_FOLDER_ID, null, null);
            }
        };

        loadTask.setOnSucceeded(event -> {
            allTasks = loadTask.getValue() == null ? List.of() : loadTask.getValue();
            applyPeriodFilter("7 ngay");
        });

        loadTask.setOnFailed(event -> mainWindow.showToast(
            "Khong tai duoc du lieu thong ke",
            "error"
        ));

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void applyPeriodFilter(String period) {
        if (allTasks == null) {
            return;
        }

        LocalDate cutoff = switch (period) {
            case "7 ngay" -> LocalDate.now().minusDays(6);
            case "30 ngay" -> LocalDate.now().minusDays(29);
            case "3 thang" -> LocalDate.now().minusMonths(3);
            default -> LocalDate.of(2000, 1, 1);
        };

        List<Task> filtered = allTasks.stream()
            .filter(task -> isRelevantForPeriod(task, cutoff, period))
            .toList();

        updateAllCharts(filtered, period);
    }

    private boolean isRelevantForPeriod(Task task, LocalDate cutoff, String period) {
        if ("Tat ca".equals(period)) {
            return true;
        }

        LocalDate referenceDate = getReferenceDate(task);
        return referenceDate == null || !referenceDate.isBefore(cutoff);
    }

    private void applyCustomFilter(LocalDate from, LocalDate to) {
        if (allTasks == null || from == null || to == null) {
            return;
        }

        List<Task> filtered = allTasks.stream()
            .filter(task -> {
                LocalDate referenceDate = getReferenceDate(task);
                if (referenceDate == null) {
                    return true;
                }
                return !referenceDate.isBefore(from) && !referenceDate.isAfter(to);
            })
            .toList();

        updateAllCharts(filtered, "custom");
    }

    private LocalDate getReferenceDate(Task task) {
        if (task.getCompletedAt() != null) {
            return task.getCompletedAt().toLocalDate();
        }
        if (task.getCreatedAt() != null) {
            return task.getCreatedAt().toLocalDate();
        }
        return task.getDueDate();
    }

    private void updateAllCharts(List<Task> tasks, String period) {
        updateSummaryCards(tasks, period);
        updateDonutChart(tasks);
        updateBarChart(tasks, period);
        updateLineChart(tasks, period);
    }

    private void updateSummaryCards(List<Task> tasks, String period) {
        long totalTasks = tasks.size();
        long completedTasks = tasks.stream()
            .filter(task -> task.getStatus() == Task.Status.DONE)
            .count();

        double completionRate = totalTasks == 0 ? 0 : (completedTasks * 100.0) / totalTasks;

        int daySpan = switch (period) {
            case "30 ngay" -> 30;
            case "3 thang" -> 90;
            case "Tat ca" -> Math.max(1, countCoveredDays(tasks));
            case "custom" -> Math.max(1, countCoveredDays(tasks));
            default -> 7;
        };
        double productivity = completedTasks / (double) daySpan;

        List<Task> completedWithDate = tasks.stream()
            .filter(task -> task.getStatus() == Task.Status.DONE && task.getCompletedAt() != null)
            .toList();
        long onTimeCompleted = completedWithDate.stream()
            .filter(task -> task.getDueDate() == null
                || !task.getCompletedAt().toLocalDate().isAfter(task.getDueDate()))
            .count();
        double onTimeRate = completedWithDate.isEmpty()
            ? 0
            : (onTimeCompleted * 100.0) / completedWithDate.size();

        int streak = calculateCompletionStreak();

        completionValueLabel.setText(String.format(Locale.US, "%.0f%%", completionRate));
        productivityValueLabel.setText(String.format(Locale.US, "%.1f", productivity));
        onTimeValueLabel.setText(String.format(Locale.US, "%.0f%%", onTimeRate));
        streakValueLabel.setText(streak + " ngay");
    }

    private int countCoveredDays(List<Task> tasks) {
        return (int) tasks.stream()
            .map(this::getReferenceDate)
            .filter(Objects::nonNull)
            .distinct()
            .count();
    }

    private int calculateCompletionStreak() {
        Set<LocalDate> completedDays = allTasks.stream()
            .map(Task::getCompletedAt)
            .filter(Objects::nonNull)
            .map(dateTime -> dateTime.toLocalDate())
            .collect(Collectors.toSet());

        int streak = 0;
        LocalDate cursor = LocalDate.now();
        while (completedDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private void updateDonutChart(List<Task> tasks) {
        long todo = tasks.stream().filter(task -> task.getStatus() == Task.Status.TODO).count();
        long inProgress = tasks.stream().filter(task -> task.getStatus() == Task.Status.IN_PROGRESS).count();
        long done = tasks.stream().filter(task -> task.getStatus() == Task.Status.DONE).count();
        long paused = tasks.stream().filter(task -> task.getStatus() == Task.Status.PAUSED).count();

        donutChart.getData().clear();
        if (tasks.isEmpty()) {
            return;
        }

        List<PieChart.Data> data = new ArrayList<>();
        if (todo > 0) {
            data.add(new PieChart.Data("Chua bat dau (" + todo + ")", todo));
        }
        if (inProgress > 0) {
            data.add(new PieChart.Data("Dang lam (" + inProgress + ")", inProgress));
        }
        if (done > 0) {
            data.add(new PieChart.Data("Hoan thanh (" + done + ")", done));
        }
        if (paused > 0) {
            data.add(new PieChart.Data("Tam hoan (" + paused + ")", paused));
        }

        donutChart.getData().addAll(data);

        String[] colors = {"#A0A0B0", "#E94560", "#00C896", "#FFB800"};
        Platform.runLater(() -> {
            for (int i = 0; i < data.size() && i < colors.length; i++) {
                if (data.get(i).getNode() != null) {
                    data.get(i).getNode().setStyle("-fx-pie-color: " + colors[i] + ";");
                }
            }
        });
    }

    private void updateBarChart(List<Task> tasks, String period) {
        barChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Hoan thanh");

        int days = switch (period) {
            case "30 ngay" -> 30;
            case "3 thang" -> 84;
            default -> 7;
        };
        int step = "3 thang".equals(period) ? 7 : 1;

        Map<String, Long> countByDay = new LinkedHashMap<>();
        for (int i = days - step; i >= 0; i -= step) {
            LocalDate date = LocalDate.now().minusDays(i);
            String key = step == 7
                ? date.minusDays(6).format(SHORT_DATE) + " - " + date.format(SHORT_DATE)
                : date.format(SHORT_DATE);

            long count = tasks.stream()
                .filter(task -> task.getStatus() == Task.Status.DONE && task.getCompletedAt() != null)
                .filter(task -> {
                    LocalDate completedDate = task.getCompletedAt().toLocalDate();
                    if (step == 7) {
                        LocalDate start = date.minusDays(6);
                        return !completedDate.isBefore(start) && !completedDate.isAfter(date);
                    }
                    return completedDate.equals(date);
                })
                .count();
            countByDay.put(key, count);
        }

        countByDay.forEach((label, count) -> series.getData().add(new XYChart.Data<>(label, count)));

        // Optimize Y-axis to show only clean integer values
        long maxCount = countByDay.values().stream().mapToLong(Long::longValue).max().orElse(0);
        NumberAxis yAxis = (NumberAxis) barChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);

        long upperBound = Math.max(5, ((maxCount + 4) / 5) * 5); // Round up to nearest multiple of 5, minimum 5
        long tickUnit = upperBound / 5;
        if (tickUnit == 0) tickUnit = 1;

        yAxis.setUpperBound(upperBound);
        yAxis.setTickUnit(tickUnit);
        yAxis.setMinorTickVisible(false);

        barChart.getData().add(series);
    }

    private void updateLineChart(List<Task> tasks, String period) {
        lineChart.getData().clear();

        int days = switch (period) {
            case "30 ngay" -> 30;
            case "3 thang" -> 90;
            default -> 7;
        };
        int step = days > 30 ? 7 : 1;

        XYChart.Series<String, Number> createdSeries = new XYChart.Series<>();
        createdSeries.setName("Tao moi");

        XYChart.Series<String, Number> doneSeries = new XYChart.Series<>();
        doneSeries.setName("Hoan thanh");

        for (int i = days - step; i >= 0; i -= step) {
            LocalDate date = LocalDate.now().minusDays(i);
            String key = date.format(SHORT_DATE);

            long created = tasks.stream()
                .filter(task -> task.getCreatedAt() != null)
                .filter(task -> isWithinBucket(task.getCreatedAt().toLocalDate(), date, step))
                .count();

            long done = tasks.stream()
                .filter(task -> task.getStatus() == Task.Status.DONE && task.getCompletedAt() != null)
                .filter(task -> isWithinBucket(task.getCompletedAt().toLocalDate(), date, step))
                .count();

            createdSeries.getData().add(new XYChart.Data<>(key, created));
            doneSeries.getData().add(new XYChart.Data<>(key, done));
        }

        lineChart.getData().addAll(createdSeries, doneSeries);

        Platform.runLater(() -> {
            if (createdSeries.getNode() != null) {
                createdSeries.getNode().setStyle("-fx-stroke: #E94560; -fx-stroke-width: 2.5;");
            }
            if (doneSeries.getNode() != null) {
                doneSeries.getNode().setStyle("-fx-stroke: #00C896; -fx-stroke-width: 2.5;");
            }
        });
    }

    private boolean isWithinBucket(LocalDate value, LocalDate bucketEnd, int step) {
        if (step == 1) {
            return value.equals(bucketEnd);
        }
        LocalDate bucketStart = bucketEnd.minusDays(step - 1L);
        return !value.isBefore(bucketStart) && !value.isAfter(bucketEnd);
    }

    private void exportChartAsPNG() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Luu bieu do");
        chooser.setInitialFileName("chart_export.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));

        File file = chooser.showSaveDialog(mainWindow.getStage());
        if (file == null) {
            return;
        }

        try {
            WritableImage image = chartsContainer.snapshot(new SnapshotParameters(), null);
            javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            mainWindow.showToast("Da xuat bieu do: " + file.getName(), "success");
        } catch (Exception exception) {
            mainWindow.showToast("Loi xuat bieu do: " + exception.getMessage(), "error");
        }
    }

    public BorderPane getRoot() {
        return root;
    }
}
