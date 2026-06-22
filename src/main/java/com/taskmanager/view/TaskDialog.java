package com.taskmanager.view;

import com.taskmanager.controller.FolderController;
import com.taskmanager.controller.TagController;
import com.taskmanager.controller.TaskController;
import com.taskmanager.dao.FolderDAO;
import com.taskmanager.model.Folder;
import com.taskmanager.model.Subtask;
import com.taskmanager.model.Tag;
import com.taskmanager.model.Task;
import com.taskmanager.util.AnimationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class TaskDialog extends Dialog<Task> {

    private final Task existingTask;
    private final boolean isEdit;

    private TextField titleField;
    private TextArea descriptionArea;
    private ComboBox<Folder> folderCombo;
    private ToggleGroup priorityGroup;
    private ToggleButton highBtn;
    private ToggleButton medBtn;
    private ToggleButton lowBtn;
    private ComboBox<String> statusCombo;
    private DatePicker startDatePicker;
    private DatePicker dueDatePicker;
    private VBox subtaskList;
    private FlowPane tagsContainer;
    private final List<Subtask> subtasks = new ArrayList<>();
    private final List<Tag> selectedTags = new ArrayList<>();
    private List<Tag> allTags = new ArrayList<>();

    public TaskDialog(Stage owner, Task existingTask) {
        this.existingTask = existingTask;
        this.isEdit = existingTask != null && existingTask.getId() > 0;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle(isEdit ? "Chỉnh sửa công việc" : "Thêm công việc mới");
        setResizable(true);

        getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        getDialogPane().getStyleClass().add("task-dialog");
        getDialogPane().setPrefWidth(580);
        getDialogPane().setPrefHeight(680);

        buildContent();
        setupResultConverter();

        if (existingTask != null) {
            populateFields();
        }

        AnimationUtil.fadeIn(getDialogPane(), 200);
    }

    private void buildContent() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox form = new VBox(20);
        form.setPadding(new Insets(24));
        form.getStyleClass().add("task-form");

        VBox titleSection = buildSection("Tiêu đề *", buildTitleField());
        VBox descSection = buildSection("Mô tả", buildDescriptionField());

        HBox folderStatusRow = new HBox(16);
        VBox folderSection = buildSection("Thư mục", buildFolderCombo());
        VBox statusSection = buildSection("Trạng thái", buildStatusCombo());
        HBox.setHgrow(folderSection, Priority.ALWAYS);
        HBox.setHgrow(statusSection, Priority.ALWAYS);
        folderStatusRow.getChildren().addAll(folderSection, statusSection);

        VBox prioritySection = buildSection("Mức độ ưu tiên", buildPrioritySelector());

        HBox dateRow = new HBox(16);
        VBox startSection = buildSection("Ngày bắt đầu", buildDatePicker(true));
        VBox dueSection = buildSection("Ngày đến hạn", buildDatePicker(false));
        HBox.setHgrow(startSection, Priority.ALWAYS);
        HBox.setHgrow(dueSection, Priority.ALWAYS);
        dateRow.getChildren().addAll(startSection, dueSection);

        VBox tagsSection = buildSection("Nhãn", buildTagsSelector());
        VBox subtasksSection = buildSection("Công việc con", buildSubtaskManager());

        form.getChildren().addAll(
            titleSection,
            descSection,
            folderStatusRow,
            prioritySection,
            dateRow,
            tagsSection,
            subtasksSection
        );

        scroll.setContent(form);

        ButtonType saveType = new ButtonType(isEdit ? "Lưu thay đổi" : "Thêm công việc", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveType, cancelType);

        Button saveBtn = (Button) getDialogPane().lookupButton(saveType);
        saveBtn.getStyleClass().add("primary-button");

        Button cancelBtn = (Button) getDialogPane().lookupButton(cancelType);
        cancelBtn.getStyleClass().add("secondary-button");

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleField.getText().trim().isEmpty()) {
                titleField.getStyleClass().add("field-error");
                AnimationUtil.shake(titleField);
                event.consume();
            }
        });

        getDialogPane().setContent(scroll);
    }

    private VBox buildSection(String label, javafx.scene.Node content) {
        VBox section = new VBox(6);
        Label sectionLabel = new Label(label);
        sectionLabel.getStyleClass().add("form-label");
        section.getChildren().addAll(sectionLabel, content);
        return section;
    }

    private TextField buildTitleField() {
        titleField = new TextField();
        titleField.setPromptText("Nhập tiêu đề công việc...");
        titleField.getStyleClass().add("form-field");
        titleField.textProperty().addListener((obs, old, val) -> titleField.getStyleClass().remove("field-error"));
        return titleField;
    }

    private TextArea buildDescriptionField() {
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Mô tả chi tiết công việc...");
        descriptionArea.getStyleClass().add("form-textarea");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        return descriptionArea;
    }

    private ComboBox<Folder> buildFolderCombo() {
        folderCombo = new ComboBox<>();
        folderCombo.getStyleClass().add("form-combo");
        folderCombo.setMaxWidth(Double.MAX_VALUE);
        folderCombo.setPromptText("Chọn thư mục...");

        try {
            List<Folder> folders = new FolderController().getAllFolders();
            folderCombo.getItems().addAll(folders.stream().filter(folder -> !folder.isDefault()).toList());
        } catch (Exception e) {
            System.err.println("Lỗi load folders: " + e.getMessage());
        }

        folderCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Folder folder, boolean empty) {
                super.updateItem(folder, empty);
                if (empty || folder == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    Circle dot = new Circle(5, Color.web(folder.getColor()));
                    Label name = new Label(folder.getName());
                    row.getChildren().addAll(dot, name);
                    setGraphic(row);
                    setText(null);
                }
            }
        });
        folderCombo.setButtonCell(folderCombo.getCellFactory().call(null));

        return folderCombo;
    }

    private ComboBox<String> buildStatusCombo() {
        statusCombo = new ComboBox<>();
        statusCombo.getStyleClass().add("form-combo");
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.getItems().addAll("Chưa bắt đầu", "Đang làm", "Hoàn thành", "Tạm hoãn");
        statusCombo.setValue("Chưa bắt đầu");
        return statusCombo;
    }

    private HBox buildPrioritySelector() {
        priorityGroup = new ToggleGroup();

        highBtn = new ToggleButton("Cao");
        medBtn = new ToggleButton("Trung bình");
        lowBtn = new ToggleButton("Thấp");

        highBtn.getStyleClass().addAll("priority-toggle", "priority-high");
        medBtn.getStyleClass().addAll("priority-toggle", "priority-medium");
        lowBtn.getStyleClass().addAll("priority-toggle", "priority-low");

        highBtn.setToggleGroup(priorityGroup);
        medBtn.setToggleGroup(priorityGroup);
        lowBtn.setToggleGroup(priorityGroup);
        medBtn.setSelected(true);

        return new HBox(10, highBtn, medBtn, lowBtn);
    }

    private DatePicker buildDatePicker(boolean isStart) {
        DatePicker picker = new DatePicker();
        picker.getStyleClass().add("form-datepicker");
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.setPromptText("DD/MM/YYYY");

        if (isStart) {
            startDatePicker = picker;
        } else {
            dueDatePicker = picker;
        }
        return picker;
    }

    private VBox buildTagsSelector() {
        VBox container = new VBox(8);

        tagsContainer = new FlowPane();
        tagsContainer.setHgap(8);
        tagsContainer.setVgap(8);
        tagsContainer.getStyleClass().add("tags-container");

        try {
            allTags = new TagController().getAllTags();
        } catch (Exception e) {
            System.err.println("Lỗi load tags: " + e.getMessage());
        }

        for (Tag tag : allTags) {
            tagsContainer.getChildren().add(createTagToggle(tag));
        }

        Button addTagBtn = new Button("+ Tag mới");
        addTagBtn.getStyleClass().add("add-tag-btn");
        addTagBtn.setOnAction(e -> showCreateTagDialog());

        container.getChildren().addAll(tagsContainer, addTagBtn);
        return container;
    }

    private ToggleButton createTagToggle(Tag tag) {
        ToggleButton tagBtn = new ToggleButton(tag.getName());
        tagBtn.getStyleClass().add("tag-toggle");
        tagBtn.setStyle("-fx-border-color: " + tag.getColor() + ";");
        tagBtn.setOnAction(e -> {
            if (tagBtn.isSelected()) {
                if (!selectedTags.contains(tag)) {
                    selectedTags.add(tag);
                }
                tagBtn.setStyle("-fx-background-color: " + tag.getColor() + "44; -fx-border-color: " + tag.getColor() + "; -fx-text-fill: " + tag.getColor() + ";");
            } else {
                selectedTags.remove(tag);
                tagBtn.setStyle("-fx-border-color: " + tag.getColor() + ";");
            }
        });
        return tagBtn;
    }

    private VBox buildSubtaskManager() {
        VBox container = new VBox(8);

        subtaskList = new VBox(6);
        subtaskList.getStyleClass().add("subtask-list");

        HBox addRow = new HBox(8);
        addRow.setAlignment(Pos.CENTER_LEFT);

        TextField subtaskInput = new TextField();
        subtaskInput.setPromptText("Thêm công việc con...");
        subtaskInput.getStyleClass().add("form-field");
        HBox.setHgrow(subtaskInput, Priority.ALWAYS);

        Button addSubBtn = new Button();
        addSubBtn.getStyleClass().add("icon-button-accent");
        FontIcon plusIcon = new FontIcon(FontAwesomeSolid.PLUS);
        plusIcon.setIconSize(13);
        addSubBtn.setGraphic(plusIcon);

        Runnable addSubtask = () -> {
            String title = subtaskInput.getText().trim();
            if (!title.isEmpty()) {
                Subtask sub = new Subtask(0, 0, title, false, subtasks.size());
                subtasks.add(sub);
                addSubtaskRow(sub);
                subtaskInput.clear();
            }
        };

        addSubBtn.setOnAction(e -> addSubtask.run());
        subtaskInput.setOnAction(e -> addSubtask.run());

        addRow.getChildren().addAll(subtaskInput, addSubBtn);
        container.getChildren().addAll(subtaskList, addRow);
        return container;
    }

    private void addSubtaskRow(Subtask subtask) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("subtask-row");
        row.setPadding(new Insets(6, 8, 6, 8));

        CheckBox check = new CheckBox();
        check.setSelected(subtask.isCompleted());
        check.setOnAction(e -> subtask.setCompleted(check.isSelected()));

        Label label = new Label(subtask.getTitle());
        label.getStyleClass().add("subtask-label");
        if (subtask.isCompleted()) {
            label.getStyleClass().add("task-done");
        }
        HBox.setHgrow(label, Priority.ALWAYS);

        Button deleteBtn = new Button();
        FontIcon trashIcon = new FontIcon(FontAwesomeSolid.TIMES);
        trashIcon.setIconSize(11);
        deleteBtn.setGraphic(trashIcon);
        deleteBtn.getStyleClass().add("delete-subtask-btn");
        deleteBtn.setOnAction(e -> {
            subtasks.remove(subtask);
            subtaskList.getChildren().remove(row);
        });

        row.getChildren().addAll(check, label, deleteBtn);
        subtaskList.getChildren().add(row);
    }

    private void populateFields() {
        titleField.setText(existingTask.getTitle());
        descriptionArea.setText(existingTask.getDescription());

        switch (existingTask.getPriority()) {
            case HIGH -> highBtn.setSelected(true);
            case LOW -> lowBtn.setSelected(true);
            default -> medBtn.setSelected(true);
        }

        statusCombo.setValue(existingTask.getStatusText());
        startDatePicker.setValue(existingTask.getStartDate());
        dueDatePicker.setValue(existingTask.getDueDate());

        folderCombo.getItems().stream()
            .filter(folder -> folder.getId() == existingTask.getFolderId())
            .findFirst()
            .ifPresent(folderCombo::setValue);

        if (existingTask.getSubtasks() != null) {
            subtasks.addAll(existingTask.getSubtasks());
            for (Subtask subtask : subtasks) {
                addSubtaskRow(subtask);
            }
        }

        if (existingTask.getTags() != null) {
            selectedTags.addAll(existingTask.getTags());
            for (javafx.scene.Node node : tagsContainer.getChildren()) {
                if (node instanceof ToggleButton tagBtn) {
                    boolean selected = existingTask.getTags().stream()
                        .anyMatch(tag -> tag.getName().equals(tagBtn.getText()));
                    if (selected) {
                        tagBtn.setSelected(true);
                        Tag matchedTag = allTags.stream()
                            .filter(tag -> tag.getName().equals(tagBtn.getText()))
                            .findFirst()
                            .orElse(null);
                        if (matchedTag != null && !selectedTags.contains(matchedTag)) {
                            selectedTags.add(matchedTag);
                        }
                        tagBtn.setStyle("-fx-background-color: #e9456044; -fx-border-color: #E94560;");
                    }
                }
            }
        }
    }

    private void setupResultConverter() {
        setResultConverter(buttonType -> {
            if (buttonType != null && buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                Task task = existingTask != null ? existingTask : new Task();
                task.setTitle(titleField.getText().trim());
                task.setDescription(descriptionArea.getText());

                if (highBtn.isSelected()) {
                    task.setPriority(Task.Priority.HIGH);
                } else if (lowBtn.isSelected()) {
                    task.setPriority(Task.Priority.LOW);
                } else {
                    task.setPriority(Task.Priority.MEDIUM);
                }

                task.setStatus(parseStatus(statusCombo.getValue()));
                task.setStartDate(startDatePicker.getValue());
                task.setDueDate(dueDatePicker.getValue());
                task.setFolderId(folderCombo.getValue() != null ? folderCombo.getValue().getId() : 0);
                task.setSubtasks(new ArrayList<>(subtasks));
                task.setTags(new ArrayList<>(selectedTags));

                try {
                    TaskController taskDAO = new TaskController();
                    if (isEdit) {
                        taskDAO.update(task);
                    } else {
                        taskDAO.insert(task);
                    }
                    return task;
                } catch (Exception e) {
                    System.err.println("Lỗi lưu task: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
    }

    private Task.Status parseStatus(String value) {
        return switch (value) {
            case "Đang làm" -> Task.Status.IN_PROGRESS;
            case "Hoàn thành" -> Task.Status.DONE;
            case "Tạm hoãn" -> Task.Status.PAUSED;
            default -> Task.Status.TODO;
        };
    }

    private void showCreateTagDialog() {
        Dialog<Tag> dialog = new Dialog<>();
        dialog.setTitle("Tạo nhãn mới");
        dialog.initOwner((Stage) getDialogPane().getScene().getWindow());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        TextField nameField = new TextField();
        nameField.getStyleClass().add("form-field");
        nameField.setPromptText("Tên nhãn");

        VBox content = new VBox(12, new Label("Tên nhãn"), nameField);
        content.setPadding(new Insets(16));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                return new Tag(0, nameField.getText().trim(), "#E94560");
            }
            return null;
        });

        dialog.showAndWait().ifPresent(tag -> {
            try {
                Tag createdTag = new TagController().insert(tag);
                allTags.add(createdTag);
                tagsContainer.getChildren().add(createTagToggle(createdTag));
            } catch (Exception e) {
                System.err.println("Lỗi tạo tag: " + e.getMessage());
            }
        });
    }
}
