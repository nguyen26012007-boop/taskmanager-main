package com.taskmanager.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện thư mục chứa task
 */
public class Folder implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private String color;
    private String icon;
    private int parentId;
    private int sortOrder;
    private boolean isDefault;
    private int taskCount; // Số task trong folder (computed)
    private List<Folder> children = new ArrayList<>();

    public Folder() {}

    public Folder(int id, String name, String color, String icon, int sortOrder, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.isDefault = isDefault;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public int getParentId() { return parentId; }
    public void setParentId(int parentId) { this.parentId = parentId; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public int getTaskCount() { return taskCount; }
    public void setTaskCount(int taskCount) { this.taskCount = taskCount; }

    public List<Folder> getChildren() { return children; }
    public void setChildren(List<Folder> children) { this.children = children; }

    @Override
    public String toString() { return name; }
}
