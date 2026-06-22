package com.taskmanager.view;

import com.taskmanager.dao.FolderDAO;
import com.taskmanager.controller.TaskController;
import com.taskmanager.model.Tag;
import com.taskmanager.model.Task;
import com.taskmanager.model.Subtask;
import com.taskmanager.service.ExportService;
import com.taskmanager.util.AnimationUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskListView {

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FILTER_ALL_STATUS = "Tất cả trạng thái";
    private static final String FILTER_ALL_PRIORITY = "Tất cả ưu tiên";
    private static final String FILTER_ALL_DUE = "Tất cả thời gian";

    private final MainWindow mainWindow;
    private final BorderPane root;

    private FlowPane cardContainer;
    private TableView<Task> tableView;
    private HBox kanbanContainer;
    private ScrollPane scrollPane;

    private TextField searchField;
    private ComboBox<String> statusFilter;
    private ComboBox<String> priorityFilter;
    private ComboBox<String> dueFilter;
    private ToggleGroup viewToggle;

    private HBox bulkActionBar;
    private Label bulkSelectionLabel;
    private Button bulkDoneButton;
    private Button bulkDeleteButton;

    private List<Task> allTasks = List.of();
    private int currentFolderId = FolderDAO.ALL_FOLDER_ID;
    private String currentView = "card";
    private final Set<Integer> selectedTaskIds = new LinkedHashSet<>();

    public TaskListView(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.root = new BorderPane();
        this.root.getStyleClass().add("task-list-root");
        buildUI();
    }

    private void buildUI() {
        root.setTop(buildToolbar());

        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setCenter(scrollPane);
    }

    private VBox buildToolbar() {
        VBox toolbar = new VBox(0);
        toolbar.getStyleClass().add("toolbar");

        HBox row1 = new HBox(12);
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.setPadding(new Insets(16, 24, 8, 24));

        Label title = new Label("Công việc");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportButton = createToolbarButton("Xuất file", FontAwesomeSolid.FILE_EXPORT);
        exportButton.setOnAction(e -> showExportDialog());

        Button addButton = new Button("+ Thêm task");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(e -> mainWindow.showAddTaskDialog(null));

        row1.getChildren().addAll(title, spacer, exportButton, addButton);

        HBox row2 = new HBox(10);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.setPadding(new Insets(0, 24, 12, 24));

        searchField = new TextField();
        searchField.setPromptText("Tìm kiếm task...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll(FILTER_ALL_STATUS, "Chưa bắt đầu", "Đang làm", "Hoàn thành", "Tạm hoãn");
        statusFilter.setValue(FILTER_ALL_STATUS);
        statusFilter.getStyleClass().add("filter-combo");
        statusFilter.setOnAction(e -> applyFilters());

        priorityFilter = new ComboBox<>();
        priorityFilter.getItems().addAll(FILTER_ALL_PRIORITY, "Cao", "Trung bình", "Thấp");
        priorityFilter.setValue(FILTER_ALL_PRIORITY);
        priorityFilter.getStyleClass().add("filter-combo");
        priorityFilter.setOnAction(e -> applyFilters());

        dueFilter = new ComboBox<>();
        dueFilter.getItems().addAll(FILTER_ALL_DUE, "Hôm nay", "7 ngày tới", "Quá hạn", "Không có hạn");
        dueFilter.setValue(FILTER_ALL_DUE);
        dueFilter.getStyleClass().add("filter-combo");
        dueFilter.setOnAction(e -> applyFilters());

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        viewToggle = new ToggleGroup();
        ToggleButton cardViewButton = createViewButton("Card", FontAwesomeSolid.TH_LARGE, "card");
        ToggleButton listViewButton = createViewButton("Danh sách", FontAwesomeSolid.LIST, "list");
        ToggleButton kanbanViewButton = createViewButton("Kanban", FontAwesomeSolid.COLUMNS, "kanban");
        cardViewButton.setSelected(true);

        HBox viewSwitcher = new HBox(0, cardViewButton, listViewButton, kanbanViewButton);
        viewSwitcher.getStyleClass().add("view-switcher");

        row2.getChildren().addAll(searchField, statusFilter, priorityFilter, dueFilter, spacer2, viewSwitcher);

        bulkActionBar = buildBulkActionBar();
        toolbar.getChildren().addAll(row1, row2, bulkActionBar);
        return toolbar;
    }

    private HBox buildBulkActionBar() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 24, 12, 24));
        row.setVisible(false);
        row.setManaged(false);

        bulkSelectionLabel = new Label("0 công việc đã chọn");
        bulkSelectionLabel.getStyleClass().add("page-subtitle");

        bulkDoneButton = createToolbarButton("Hoàn thành đã chọn", FontAwesomeSolid.CHECK);
        bulkDoneButton.setOnAction(e -> markSelectedTasksDone());

        bulkDeleteButton = createToolbarButton("Xóa đã chọn", FontAwesomeSolid.TRASH);
        bulkDeleteButton.getStyleClass().add("danger");
        bulkDeleteButton.setOnAction(e -> confirmDelete(new ArrayList<>(selectedTaskIds)));

        Button clearButton = createToolbarButton("Bỏ chọn", FontAwesomeSolid.TIMES);
        clearButton.setOnAction(e -> {
            selectedTaskIds.clear();
            if (tableView != null) {
                tableView.refresh();
            }
            updateBulkActionState();
        });

        row.getChildren().addAll(bulkSelectionLabel, bulkDoneButton, bulkDeleteButton, clearButton);
        return row;
    }

    private Button createToolbarButton(String text, FontAwesomeSolid icon) {
        Button button = new Button(text);
        button.getStyleClass().add("toolbar-button");
        FontIcon iconNode = new FontIcon(icon);
        iconNode.setIconSize(13);
        button.setGraphic(iconNode);
        return button;
    }

    private ToggleButton createViewButton(String tooltip, FontAwesomeSolid icon, String viewName) {
        ToggleButton button = new ToggleButton();
        button.getStyleClass().add("view-toggle-btn");
        button.setToggleGroup(viewToggle);
        FontIcon iconNode = new FontIcon(icon);
        iconNode.setIconSize(14);
        button.setGraphic(iconNode);
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(e -> {
            if (button.isSelected()) {
                currentView = viewName;
                if (!"list".equals(currentView)) {
                    selectedTaskIds.clear();
                }
                renderCurrentView();
            }
        });
        return button;
    }

    public void refresh() {
        javafx.concurrent.Task<List<Task>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Task> call() throws Exception {
                return new TaskController().getAllTasks(currentFolderId, null, null);
            }
        };

        loadTask.setOnSucceeded(e -> {
            allTasks = loadTask.getValue();
            selectedTaskIds.retainAll(allTasks.stream().map(Task::getId).collect(Collectors.toSet()));
            renderCurrentView();
        });

        loadTask.setOnFailed(e -> mainWindow.showToast(
            "Lỗi tải dữ liệu: " + loadTask.getException().getMessage(),
            "error"
        ));

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void renderCurrentView() {
        List<Task> filtered = getFilteredTasks();
        switch (currentView) {
            case "card" -> renderCardView(filtered);
            case "list" -> renderListView(filtered);
            case "kanban" -> renderKanbanView(filtered);
            default -> renderCardView(filtered);
        }
        updateBulkActionState();
    }

    private void renderCardView(List<Task> tasks) {
        cardContainer = new FlowPane();
        cardContainer.setHgap(16);
        cardContainer.setVgap(16);
        cardContainer.setPadding(new Insets(20, 24, 24, 24));
        cardContainer.getStyleClass().add("card-container");

        if (tasks.isEmpty()) {
            cardContainer.getChildren().add(buildEmptyState());
        } else {
            for (Task task : tasks) {
                cardContainer.getChildren().add(buildTaskCard(task));
            }
            AnimationUtil.staggerFadeIn(cardContainer.getChildren().stream().toList(), 50);
        }

        scrollPane.setContent(cardContainer);
    }

    private VBox buildTaskCard(Task task) {
        VBox card = new VBox(10);
        card.getStyleClass().add("task-card");
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-border-color: transparent transparent transparent " + task.getPriorityColor() + "; -fx-border-width: 0 0 0 3;");

        HBox cardHeader = new HBox(8);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        Label priorityBadge = new Label(task.getPriorityText());
        priorityBadge.getStyleClass().addAll("priority-badge", "priority-" + task.getPriority().name().toLowerCase());

        Label statusBadge = new Label(task.getStatusText());
        statusBadge.getStyleClass().addAll("status-badge", "status-" + task.getStatus().name().toLowerCase());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button menuButton = new Button("⋮");
        menuButton.getStyleClass().add("card-menu-btn");
        ContextMenu cardMenu = buildTaskContextMenu(task);
        menuButton.setOnAction(e -> cardMenu.show(menuButton, javafx.geometry.Side.BOTTOM, 0, 0));

        cardHeader.getChildren().addAll(priorityBadge, statusBadge, headerSpacer, menuButton);

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);
        if (task.getStatus() == Task.Status.DONE) {
            titleLabel.getStyleClass().add("task-done");
        }

        card.getChildren().addAll(cardHeader, titleLabel);

        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            Label descriptionLabel = new Label(task.getDescription());
            descriptionLabel.getStyleClass().add("card-description");
            descriptionLabel.setWrapText(true);
            descriptionLabel.setMaxHeight(40);
            card.getChildren().add(descriptionLabel);
        }

        if (!task.getTags().isEmpty()) {
            HBox tagsBox = new HBox(6);
            for (Tag tag : task.getTags()) {
                Label tagChip = new Label(tag.getName());
                tagChip.getStyleClass().add("tag-chip");
                tagChip.setStyle("-fx-background-color: " + tag.getColor() + "33; -fx-text-fill: " + tag.getColor() + ";");
                tagsBox.getChildren().add(tagChip);
            }
            card.getChildren().add(tagsBox);
        }

        if (!task.getSubtasks().isEmpty()) {
            long completedSubtasks = task.getSubtasks().stream().filter(Subtask::isCompleted).count();
            int totalSubtasks = task.getSubtasks().size();

            HBox progressRow = new HBox(8);
            progressRow.setAlignment(Pos.CENTER_LEFT);

            ProgressBar progressBar = new ProgressBar(task.getCompletionPercent() / 100.0);
            progressBar.getStyleClass().add("task-progress-bar");
            HBox.setHgrow(progressBar, Priority.ALWAYS);

            Label progressLabel = new Label(completedSubtasks + "/" + totalSubtasks);
            progressLabel.getStyleClass().add("progress-label");

            progressRow.getChildren().addAll(progressBar, progressLabel);
            card.getChildren().add(progressRow);
        }

        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("card-footer");

        if (task.getDueDate() != null) {
            Label dueDateLabel = new Label("Hạn: " + task.getDueDate().format(SHORT_DATE));
            dueDateLabel.getStyleClass().add(task.isOverdue() ? "due-date-overdue" : "due-date");
            footer.getChildren().add(dueDateLabel);
        }

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        footer.getChildren().add(footerSpacer);

        if (task.getFolderName() != null && !task.getFolderName().isBlank()) {
            Label folderLabel = new Label(task.getFolderName());
            folderLabel.getStyleClass().add("folder-label");
            footer.getChildren().add(folderLabel);
        }

        card.getChildren().addAll(footer, buildTaskActions(task));

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                mainWindow.showAddTaskDialog(task);
            }
        });

        if (task.isOverdue()) {
            card.getStyleClass().add("task-overdue");
        }

        AnimationUtil.addHoverEffect(card, task.getPriorityColor());
        return card;
    }

    private ContextMenu buildTaskContextMenu(Task task) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("context-menu-dark");

        MenuItem editItem = new MenuItem("Chỉnh sửa");
        editItem.setOnAction(e -> mainWindow.showAddTaskDialog(task));

        MenuItem doneItem = new MenuItem("Đánh dấu hoàn thành");
        doneItem.setOnAction(e -> markTaskDone(task));

        MenuItem deleteItem = new MenuItem("Xóa");
        deleteItem.getStyleClass().add("danger");
        deleteItem.setOnAction(e -> confirmDelete(List.of(task.getId())));

        menu.getItems().addAll(editItem, doneItem, new SeparatorMenuItem(), deleteItem);
        return menu;
    }

    private HBox buildTaskActions(Task task) {
        Button completeButton = createTaskActionButton("Hoàn thành", FontAwesomeSolid.CHECK);
        completeButton.setDisable(task.getStatus() == Task.Status.DONE);
        completeButton.setOnAction(e -> markTaskDone(task));

        Button editButton = createTaskActionButton("Sửa", FontAwesomeSolid.EDIT);
        editButton.setOnAction(e -> mainWindow.showAddTaskDialog(task));

        Button deleteButton = createTaskActionButton("Xóa", FontAwesomeSolid.TRASH);
        deleteButton.getStyleClass().add("danger");
        deleteButton.setOnAction(e -> confirmDelete(List.of(task.getId())));

        HBox actionRow = new HBox(8, completeButton, editButton, deleteButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        return actionRow;
    }

    private Button createTaskActionButton(String text, FontAwesomeSolid icon) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        FontIcon iconNode = new FontIcon(icon);
        iconNode.setIconSize(12);
        button.setGraphic(iconNode);
        return button;
    }

    private void markTaskDone(Task task) {
        try {
            new TaskController().updateStatus(task.getId(), Task.Status.DONE);
            selectedTaskIds.remove(task.getId());
            mainWindow.refreshAllDataViews();
            mainWindow.showToast("Đã đánh dấu hoàn thành", "success");
        } catch (Exception e) {
            mainWindow.showToast("Không thể cập nhật trạng thái task", "error");
        }
    }

    private void markSelectedTasksDone() {
        if (selectedTaskIds.isEmpty()) {
            return;
        }

        try {
            TaskController dao = new TaskController();
            for (Integer id : selectedTaskIds) {
                dao.updateStatus(id, Task.Status.DONE);
            }
            selectedTaskIds.clear();
            mainWindow.refreshAllDataViews();
            mainWindow.showToast("Đã hoàn thành các task đã chọn", "success");
        } catch (Exception e) {
            mainWindow.showToast("Không thể hoàn thành hàng loạt", "error");
        }
    }

    private void renderListView(List<Task> tasks) {
        tableView = new TableView<>();
        tableView.getStyleClass().add("task-table");
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getItems().setAll(tasks);
        tableView.setPlaceholder(buildEmptyState());
        tableView.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    mainWindow.showAddTaskDialog(row.getItem());
                }
            });
            return row;
        });

        CheckBox selectAllBox = new CheckBox();
        selectAllBox.setOnAction(e -> {
            if (selectAllBox.isSelected()) {
                tasks.forEach(task -> selectedTaskIds.add(task.getId()));
            } else {
                tasks.forEach(task -> selectedTaskIds.remove(task.getId()));
            }
            tableView.refresh();
            updateBulkActionState();
        });

        TableColumn<Task, Task> selectColumn = new TableColumn<>();
        selectColumn.setGraphic(selectAllBox);
        selectColumn.setPrefWidth(42);
        selectColumn.setSortable(false);
        selectColumn.setReorderable(false);
        selectColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        selectColumn.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    Task task = getItem();
                    if (task == null) {
                        return;
                    }
                    if (checkBox.isSelected()) {
                        selectedTaskIds.add(task.getId());
                    } else {
                        selectedTaskIds.remove(task.getId());
                    }
                    updateBulkActionState();
                });
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(selectedTaskIds.contains(item.getId()));
                    setGraphic(checkBox);
                }
            }
        });

        TableColumn<Task, String> titleColumn = new TableColumn<>("Tiêu đề");
        titleColumn.setCellValueFactory(p -> p.getValue().titleProperty());
        titleColumn.setPrefWidth(250);

        TableColumn<Task, String> priorityColumn = new TableColumn<>("Ưu tiên");
        priorityColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getPriorityText()));
        priorityColumn.setPrefWidth(110);
        priorityColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Task task = getTableView().getItems().get(getIndex());
                Label badge = new Label(item);
                badge.getStyleClass().addAll("priority-badge", "priority-" + task.getPriority().name().toLowerCase());
                setGraphic(badge);
                setText(null);
            }
        });

        TableColumn<Task, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getStatusText()));
        statusColumn.setPrefWidth(130);

        TableColumn<Task, String> dueDateColumn = new TableColumn<>("Đến hạn");
        dueDateColumn.setCellValueFactory(p -> new SimpleStringProperty(
            p.getValue().getDueDate() != null ? p.getValue().getDueDate().format(FULL_DATE) : ""
        ));
        dueDateColumn.setPrefWidth(120);

        TableColumn<Task, String> folderColumn = new TableColumn<>("Thư mục");
        folderColumn.setCellValueFactory(p -> p.getValue().folderNameProperty());
        folderColumn.setPrefWidth(140);

        TableColumn<Task, Void> actionColumn = new TableColumn<>("Thao tác");
        actionColumn.setPrefWidth(290);
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final HBox actions = new HBox(8);
            private final Button completeButton = createTaskActionButton("Hoàn thành", FontAwesomeSolid.CHECK);
            private final Button editButton = createTaskActionButton("Sửa", FontAwesomeSolid.EDIT);
            private final Button deleteButton = createTaskActionButton("Xóa", FontAwesomeSolid.TRASH);

            {
                actions.setAlignment(Pos.CENTER);
                deleteButton.getStyleClass().add("danger");
                completeButton.setOnAction(e -> markTaskDone(getTableView().getItems().get(getIndex())));
                editButton.setOnAction(e -> mainWindow.showAddTaskDialog(getTableView().getItems().get(getIndex())));
                deleteButton.setOnAction(e -> confirmDelete(List.of(getTableView().getItems().get(getIndex()).getId())));
                actions.getChildren().addAll(completeButton, editButton, deleteButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Task task = getTableView().getItems().get(getIndex());
                    completeButton.setDisable(task.getStatus() == Task.Status.DONE);
                    setGraphic(actions);
                }
            }
        });

        tableView.getColumns().addAll(
            selectColumn,
            titleColumn,
            priorityColumn,
            statusColumn,
            dueDateColumn,
            folderColumn,
            actionColumn
        );
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox wrapper = new VBox(tableView);
        wrapper.setPadding(new Insets(20, 24, 24, 24));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        scrollPane.setContent(wrapper);
    }

    private void renderKanbanView(List<Task> tasks) {
        kanbanContainer = new HBox(16);
        kanbanContainer.setPadding(new Insets(20, 24, 24, 24));
        kanbanContainer.getStyleClass().add("kanban-container");

        Task.Status[] statuses = Task.Status.values();
        String[] titles = {"Chưa bắt đầu", "Đang làm", "Hoàn thành", "Tạm hoãn"};
        String[] colors = {"#A0A0B0", "#E94560", "#00C896", "#FFB800"};

        for (int i = 0; i < statuses.length; i++) {
            Task.Status status = statuses[i];
            List<Task> columnTasks = tasks.stream().filter(t -> t.getStatus() == status).collect(Collectors.toList());
            VBox column = buildKanbanColumn(titles[i], colors[i], status, columnTasks);
            HBox.setHgrow(column, Priority.ALWAYS);
            kanbanContainer.getChildren().add(column);
        }

        ScrollPane horizontalScroll = new ScrollPane(kanbanContainer);
        horizontalScroll.setFitToHeight(true);
        horizontalScroll.getStyleClass().add("transparent-scroll");
        scrollPane.setContent(horizontalScroll);
    }

    private VBox buildKanbanColumn(String title, String color, Task.Status status, List<Task> tasks) {
        VBox column = new VBox(8);
        column.getStyleClass().add("kanban-column");
        column.setPadding(new Insets(12));
        column.setMinWidth(240);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(6, Color.web(color));
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kanban-title");

        Label countLabel = new Label(String.valueOf(tasks.size()));
        countLabel.getStyleClass().add("kanban-count");
        countLabel.setStyle("-fx-background-color: " + color + "33; -fx-text-fill: " + color + ";");

        header.getChildren().addAll(dot, titleLabel, countLabel);
        column.getChildren().add(header);

        VBox dropZone = new VBox(8);
        dropZone.setFillWidth(true);
        VBox.setVgrow(dropZone, Priority.ALWAYS);
        dropZone.setMinHeight(120);

        dropZone.setOnDragOver(e -> {
            if (e.getGestureSource() != dropZone && e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        dropZone.setOnDragEntered(e -> {
            if (e.getDragboard().hasString()) {
                dropZone.setStyle("-fx-background-color: rgba(47,111,237,0.08); -fx-background-radius: 12;");
            }
            e.consume();
        });
        dropZone.setOnDragExited(e -> {
            dropZone.setStyle("");
            e.consume();
        });
        dropZone.setOnDragDropped(e -> {
            boolean success = false;
            Dragboard dragboard = e.getDragboard();
            if (dragboard.hasString()) {
                try {
                    int taskId = Integer.parseInt(dragboard.getString());
                    moveTaskToStatus(taskId, status);
                    success = true;
                } catch (NumberFormatException ignored) {
                }
            }
            dropZone.setStyle("");
            e.setDropCompleted(success);
            e.consume();
        });

        for (Task task : tasks) {
            dropZone.getChildren().add(buildMiniCard(task));
        }

        Button addToColumnButton = new Button("+ Thêm task");
        addToColumnButton.getStyleClass().add("kanban-add-btn");
        addToColumnButton.setMaxWidth(Double.MAX_VALUE);
        addToColumnButton.setOnAction(e -> {
            Task newTask = new Task();
            newTask.setStatus(status);
            mainWindow.showAddTaskDialog(newTask);
        });

        column.getChildren().addAll(dropZone, addToColumnButton);
        return column;
    }

    private VBox buildMiniCard(Task task) {
        VBox card = new VBox(6);
        card.getStyleClass().add("kanban-card");
        card.setPadding(new Insets(10));
        card.setStyle("-fx-border-color: transparent transparent transparent " + task.getPriorityColor() + "; -fx-border-width: 0 0 0 3;");

        Label title = new Label(task.getTitle());
        title.getStyleClass().add("kanban-card-title");
        title.setWrapText(true);

        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label priority = new Label(task.getPriorityText());
        priority.getStyleClass().addAll("priority-badge", "priority-" + task.getPriority().name().toLowerCase());
        priority.setStyle("-fx-font-size: 10; -fx-padding: 2 6;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        footer.getChildren().addAll(priority, footerSpacer);
        if (task.getDueDate() != null) {
            Label due = new Label(task.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM")));
            due.getStyleClass().add(task.isOverdue() ? "due-date-overdue" : "due-date");
            footer.getChildren().add(due);
        }

        card.getChildren().addAll(title, footer, buildTaskActions(task));
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                mainWindow.showAddTaskDialog(task);
            }
        });
        card.setOnDragDetected(e -> {
            Dragboard dragboard = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(task.getId()));
            dragboard.setContent(content);
            e.consume();
        });

        AnimationUtil.addHoverEffect(card, task.getPriorityColor());
        return card;
    }

    private void moveTaskToStatus(int taskId, Task.Status targetStatus) {
        Task task = allTasks.stream().filter(item -> item.getId() == taskId).findFirst().orElse(null);
        if (task == null || task.getStatus() == targetStatus) {
            return;
        }

        try {
            new TaskController().updateStatus(taskId, targetStatus);
            selectedTaskIds.remove(taskId);
            mainWindow.refreshAllDataViews();
            mainWindow.showToast("Đã chuyển task sang " + targetStatusText(targetStatus), "success");
        } catch (Exception e) {
            mainWindow.showToast("Không thể kéo thả task: " + e.getMessage(), "error");
        }
    }

    private String targetStatusText(Task.Status status) {
        return switch (status) {
            case TODO -> "Chưa bắt đầu";
            case IN_PROGRESS -> "Đang làm";
            case DONE -> "Hoàn thành";
            case PAUSED -> "Tạm hoãn";
        };
    }

    private void applyFilters() {
        renderCurrentView();
    }

    private List<Task> getFilteredTasks() {
        if (allTasks == null) {
            return List.of();
        }

        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String statusValue = statusFilter.getValue() != null ? statusFilter.getValue().trim() : "";
        String priorityValue = priorityFilter.getValue() != null ? priorityFilter.getValue().trim() : "";
        String dueValue = dueFilter.getValue() != null ? dueFilter.getValue().trim() : "";

        boolean matchAllStatus = statusValue.isBlank() || FILTER_ALL_STATUS.equals(statusValue);
        boolean matchAllPriority = priorityValue.isBlank() || FILTER_ALL_PRIORITY.equals(priorityValue);
        boolean matchAllDue = dueValue.isBlank() || FILTER_ALL_DUE.equals(dueValue);

        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        return allTasks.stream()
            .filter(task -> searchText.isEmpty()
                || task.getTitle().toLowerCase().contains(searchText)
                || (task.getDescription() != null && task.getDescription().toLowerCase().contains(searchText)))
            .filter(task -> matchAllStatus || task.getStatusText().equals(statusValue))
            .filter(task -> matchAllPriority || task.getPriorityText().equals(priorityValue))
            .filter(task -> {
                if (matchAllDue) {
                    return true;
                }
                if ("Hôm nay".equals(dueValue)) {
                    return task.getDueDate() != null && task.getDueDate().equals(today);
                }
                if ("7 ngày tới".equals(dueValue)) {
                    return task.getDueDate() != null
                        && !task.getDueDate().isBefore(today)
                        && !task.getDueDate().isAfter(nextWeek);
                }
                if ("Quá hạn".equals(dueValue)) {
                    return task.isOverdue();
                }
                if ("Không có hạn".equals(dueValue)) {
                    return task.getDueDate() == null;
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private void updateBulkActionState() {
        boolean visible = "list".equals(currentView) && !selectedTaskIds.isEmpty();
        bulkActionBar.setVisible(visible);
        bulkActionBar.setManaged(visible);
        bulkSelectionLabel.setText(selectedTaskIds.size() + " công việc đã chọn");
        bulkDoneButton.setDisable(selectedTaskIds.isEmpty());
        bulkDeleteButton.setDisable(selectedTaskIds.isEmpty());
    }

    public void filterByFolder(int folderId) {
        currentFolderId = folderId;
        selectedTaskIds.clear();
        refresh();
    }

    private void confirmDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Xóa " + ids.size() + " công việc?");
        alert.setContentText("Hành động này không thể hoàn tác.");
        alert.initOwner(mainWindow.getStage());
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    new TaskController().deleteMultiple(ids);
                    selectedTaskIds.removeAll(ids);
                    mainWindow.refreshAllDataViews();
                    mainWindow.showToast("Đã xóa " + ids.size() + " task", "success");
                } catch (Exception e) {
                    mainWindow.showToast("Lỗi xóa task: " + e.getMessage(), "error");
                }
            }
        });
    }

    private void showExportDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Xuất dữ liệu");
        dialog.setHeaderText("Chọn định dạng xuất file");
        dialog.initOwner(mainWindow.getStage());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        ToggleGroup formatGroup = new ToggleGroup();
        RadioButton csvButton = new RadioButton("CSV");
        RadioButton xlsxButton = new RadioButton("Excel (.xlsx)");
        RadioButton pdfButton = new RadioButton("PDF");
        csvButton.setToggleGroup(formatGroup);
        xlsxButton.setToggleGroup(formatGroup);
        pdfButton.setToggleGroup(formatGroup);
        csvButton.setSelected(true);

        VBox content = new VBox(12, new Label("Định dạng:"), csvButton, xlsxButton, pdfButton);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (csvButton.isSelected()) {
                    return "csv";
                }
                if (xlsxButton.isSelected()) {
                    return "xlsx";
                }
                return "pdf";
            }
            return null;
        });

        dialog.showAndWait().ifPresent(format -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Lưu file");
            fileChooser.setInitialFileName("tasks." + format);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.toUpperCase(), "*." + format));
            File file = fileChooser.showSaveDialog(mainWindow.getStage());

            if (file != null) {
                List<Task> exportTasks = getFilteredTasks();
                ExportService exportService = new ExportService();
                try {
                    switch (format) {
                        case "csv" -> exportService.exportCSV(exportTasks, file.getAbsolutePath());
                        case "xlsx" -> exportService.exportExcel(exportTasks, file.getAbsolutePath());
                        case "pdf" -> exportService.exportPDF(exportTasks, file.getAbsolutePath());
                        default -> {
                        }
                    }
                    mainWindow.showToast("Xuất file thành công: " + file.getName(), "success");
                } catch (Exception e) {
                    mainWindow.showToast("Lỗi xuất file: " + e.getMessage(), "error");
                }
            }
        });
    }

    private VBox buildEmptyState() {
        VBox empty = new VBox(16);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(60));

        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size: 48;");

        Label title = new Label("Chưa có công việc nào");
        title.getStyleClass().add("empty-state-title");

        Label subtitle = new Label("Nhấn + Thêm task để bắt đầu");
        subtitle.getStyleClass().add("empty-state-subtitle");

        Button addButton = new Button("+ Thêm task đầu tiên");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(e -> mainWindow.showAddTaskDialog(null));

        empty.getChildren().addAll(icon, title, subtitle, addButton);
        return empty;
    }

    public BorderPane getRoot() {
        return root;
    }
}
