package com.taskmanager.model;

import java.io.Serializable;

/**
 * Model class đại diện cho công việc con (Subtask)
 */
public class Subtask implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private int taskId;
    private String title;
    private boolean completed;
    private int sortOrder;

    public Subtask() {}

    public Subtask(int id, int taskId, String title, boolean completed, int sortOrder) {
        this.id = id;
        this.taskId = taskId;
        this.title = title;
        this.completed = completed;
        this.sortOrder = sortOrder;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
