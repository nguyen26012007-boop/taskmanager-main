package com.taskmanager.dao;

import com.taskmanager.model.Tag;
import com.taskmanager.util.DBConnection;
import com.taskmanager.util.SessionContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TagDAO {

    public List<Tag> getAllTags() throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tags WHERE user_id = ? ORDER BY name";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tags.add(new Tag(rs.getInt("id"), rs.getString("name"), rs.getString("color")));
            }
        }
        return tags;
    }

    public Tag insert(Tag tag) throws SQLException {
        String existingSql = "SELECT id, name, color FROM tags WHERE user_id = ? AND lower(name) = lower(?)";
        try (PreparedStatement existing = DBConnection.getConnection().prepareStatement(existingSql)) {
            existing.setInt(1, SessionContext.getCurrentUserId());
            existing.setString(2, tag.getName());
            ResultSet rs = existing.executeQuery();
            if (rs.next()) {
                return new Tag(rs.getInt("id"), rs.getString("name"), rs.getString("color"));
            }
        }

        String sql = "INSERT INTO tags (name, color, user_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tag.getName());
            ps.setString(2, tag.getColor());
            ps.setInt(3, SessionContext.getCurrentUserId());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                tag.setId(keys.getInt(1));
            }
        }
        return tag;
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
            "DELETE FROM tags WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, SessionContext.getCurrentUserId());
            ps.executeUpdate();
        }
    }
}
