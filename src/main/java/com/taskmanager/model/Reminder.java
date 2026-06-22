package com.taskmanager.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model đại diện nhắc nhở cho task
 */
public class Reminder implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum RepeatType { NONE, DAILY, WEEKLY }

    private int id;
    private int taskId;
    private String taskTitle; // Để hiển thị notification
    private LocalDateTime remindAt;
    private boolean sent;
    private RepeatType repeatType;

    public Reminder() {}

    public Reminder(int id, int taskId, LocalDateTime remindAt, boolean sent, RepeatType repeatType) {
        this.id = id;
        this.taskId = taskId;
        this.remindAt = remindAt;
        this.sent = sent;
        this.repeatType = repeatType != null ? repeatType : RepeatType.NONE;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public LocalDateTime getRemindAt() { return remindAt; }
    public void setRemindAt(LocalDateTime remindAt) { this.remindAt = remindAt; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }

    public RepeatType getRepeatType() { return repeatType != null ? repeatType : RepeatType.NONE; }
    public void setRepeatType(RepeatType repeatType) { this.repeatType = repeatType != null ? repeatType : RepeatType.NONE; }
}
