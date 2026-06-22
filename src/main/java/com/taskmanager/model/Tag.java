package com.taskmanager.model;

import java.io.Serializable;

/**
 * Model đại diện nhãn (tag) gắn vào task
 */
public class Tag implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private String color;

    public Tag() {}

    public Tag(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    @Override
    public String toString() { return name; }
}
