package com.taskmanager.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện cho một công việc.
 */
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Priority { HIGH, MEDIUM, LOW }
    public enum Status { TODO, IN_PROGRESS, DONE, PAUSED }

    private transient IntegerProperty id = new SimpleIntegerProperty();
    private transient StringProperty title = new SimpleStringProperty();
    private transient StringProperty description = new SimpleStringProperty();
    private transient IntegerProperty folderId = new SimpleIntegerProperty();
    private transient StringProperty folderName = new SimpleStringProperty();
    private transient ObjectProperty<Priority> priority = new SimpleObjectProperty<>(Priority.MEDIUM);
    private transient ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.TODO);
    private transient ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>();
    private transient ObjectProperty<LocalDate> dueDate = new SimpleObjectProperty<>();
    private transient ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private transient ObjectProperty<LocalDateTime> updatedAt = new SimpleObjectProperty<>();
    private transient ObjectProperty<LocalDateTime> completedAt = new SimpleObjectProperty<>();

    private List<Subtask> subtasks = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    public Task() {}

    public Task(int id, String title, String description, int folderId,
                Priority priority, Status status, LocalDate startDate, LocalDate dueDate) {
        setId(id);
        setTitle(title);
        setDescription(description);
        setFolderId(folderId);
        setPriority(priority);
        setStatus(status);
        setStartDate(startDate);
        setDueDate(dueDate);
    }

    // ===== Custom Serialization (JavaFX Property không Serializable) =====

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(getId());
        out.writeObject(getTitle());
        out.writeObject(getDescription());
        out.writeInt(getFolderId());
        out.writeObject(getFolderName());
        out.writeObject(getPriority());
        out.writeObject(getStatus());
        out.writeObject(getStartDate());
        out.writeObject(getDueDate());
        out.writeObject(getCreatedAt());
        out.writeObject(getUpdatedAt());
        out.writeObject(getCompletedAt());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Khởi tạo lại JavaFX Property sau khi deserialize
        id = new SimpleIntegerProperty(in.readInt());
        title = new SimpleStringProperty((String) in.readObject());
        description = new SimpleStringProperty((String) in.readObject());
        folderId = new SimpleIntegerProperty(in.readInt());
        folderName = new SimpleStringProperty((String) in.readObject());
        priority = new SimpleObjectProperty<>((Priority) in.readObject());
        status = new SimpleObjectProperty<>((Status) in.readObject());
        startDate = new SimpleObjectProperty<>((LocalDate) in.readObject());
        dueDate = new SimpleObjectProperty<>((LocalDate) in.readObject());
        createdAt = new SimpleObjectProperty<>((LocalDateTime) in.readObject());
        updatedAt = new SimpleObjectProperty<>((LocalDateTime) in.readObject());
        completedAt = new SimpleObjectProperty<>((LocalDateTime) in.readObject());
    }

    // ===== Business Logic =====

    public double getCompletionPercent() {
        if (subtasks.isEmpty()) {
            return getStatus() == Status.DONE ? 100.0 : 0.0;
        }
        long done = subtasks.stream().filter(Subtask::isCompleted).count();
        return (double) done / subtasks.size() * 100.0;
    }

    public boolean isOverdue() {
        return getDueDate() != null
            && getDueDate().isBefore(LocalDate.now())
            && getStatus() != Status.DONE;
    }

    public boolean isDueSoon() {
        if (getDueDate() == null || isOverdue()) {
            return false;
        }
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return !getDueDate().isAfter(tomorrow) && getStatus() != Status.DONE;
    }

    public String getPriorityColor() {
        return switch (getPriority()) {
            case HIGH -> "#E94560";
            case MEDIUM -> "#FFB800";
            case LOW -> "#00C896";
        };
    }

    public String getStatusText() {
        return switch (getStatus()) {
            case TODO -> "Chưa bắt đầu";
            case IN_PROGRESS -> "Đang làm";
            case DONE -> "Hoàn thành";
            case PAUSED -> "Tạm hoãn";
        };
    }

    public String getPriorityText() {
        return switch (getPriority()) {
            case HIGH -> "Cao";
            case MEDIUM -> "Trung bình";
            case LOW -> "Thấp";
        };
    }

    // ===== Property Accessors =====

    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getTitle() { return title.get(); }
    public void setTitle(String title) { this.title.set(title); }
    public StringProperty titleProperty() { return title; }

    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public StringProperty descriptionProperty() { return description; }

    public int getFolderId() { return folderId.get(); }
    public void setFolderId(int folderId) { this.folderId.set(folderId); }
    public IntegerProperty folderIdProperty() { return folderId; }

    public String getFolderName() { return folderName.get(); }
    public void setFolderName(String folderName) { this.folderName.set(folderName); }
    public StringProperty folderNameProperty() { return folderName; }

    public Priority getPriority() { return priority.get(); }
    public void setPriority(Priority priority) { this.priority.set(priority); }
    public ObjectProperty<Priority> priorityProperty() { return priority; }

    public Status getStatus() { return status.get(); }
    public void setStatus(Status status) { this.status.set(status); }
    public ObjectProperty<Status> statusProperty() { return status; }

    public LocalDate getStartDate() { return startDate.get(); }
    public void setStartDate(LocalDate startDate) { this.startDate.set(startDate); }
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }

    public LocalDate getDueDate() { return dueDate.get(); }
    public void setDueDate(LocalDate dueDate) { this.dueDate.set(dueDate); }
    public ObjectProperty<LocalDate> dueDateProperty() { return dueDate; }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }

    public LocalDateTime getUpdatedAt() { return updatedAt.get(); }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt.set(updatedAt); }

    public LocalDateTime getCompletedAt() { return completedAt.get(); }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt.set(completedAt); }

    public List<Subtask> getSubtasks() { return subtasks; }
    public void setSubtasks(List<Subtask> subtasks) { this.subtasks = subtasks; }

    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) { this.tags = tags; }

    @Override
    public String toString() {
        return getTitle();
    }
}
