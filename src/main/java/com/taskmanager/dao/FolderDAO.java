package com.taskmanager.dao;

import com.taskmanager.model.Folder;
import com.taskmanager.util.DBConnection;
import com.taskmanager.util.SessionContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FolderDAO {

    public static final int ALL_FOLDER_ID = -1;
    public static final int TODAY_FOLDER_ID = -2;
    public static final int IMPORTANT_FOLDER_ID = -3;
    public static final int DONE_FOLDER_ID = -4;

    public List<Folder> getAllFolders() throws SQLException {
        List<Folder> folders = new ArrayList<>();
        folders.add(createVirtualFolder(ALL_FOLDER_ID, "Tất cả", "#A0A0B0", "list", getTaskCount("1=1")));
        folders.add(createVirtualFolder(TODAY_FOLDER_ID, "Hôm nay", "#E94560", "today",
            getTaskCount("DATE(due_date) = CURDATE() AND status != 'DONE'")));
        folders.add(createVirtualFolder(IMPORTANT_FOLDER_ID, "Quan trọng", "#FFB800", "star",
            getTaskCount("priority = 'HIGH' AND status != 'DONE'")));
        folders.add(createVirtualFolder(DONE_FOLDER_ID, "Hoàn thành", "#00C896", "check-circle",
            getTaskCount("status = 'DONE'")));

        String sql = """
            SELECT f.*,
                   (SELECT COUNT(*) FROM tasks t WHERE t.folder_id = f.id AND t.user_id = ? AND t.status != 'DONE') AS task_count
            FROM folders f
            WHERE f.user_id = ?
            ORDER BY f.sort_order, f.name
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, SessionContext.getCurrentUserId());
            ps.setInt(2, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                folders.add(mapResultSet(rs));
            }
        }
        return folders;
    }

    public Folder getById(int id) throws SQLException {
        String sql = "SELECT * FROM folders WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }

    public int insert(Folder folder) throws SQLException {
        String sql = """
            INSERT INTO folders (name, color, icon, parent_id, sort_order, is_default, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, folder.getName());
            ps.setString(2, folder.getColor());
            ps.setString(3, folder.getIcon());
            ps.setObject(4, folder.getParentId() > 0 ? folder.getParentId() : null);
            ps.setInt(5, folder.getSortOrder());
            ps.setInt(6, 0);
            ps.setInt(7, SessionContext.getCurrentUserId());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                folder.setId(keys.getInt(1));
                return folder.getId();
            }
        }
        return -1;
    }

    public void update(Folder folder) throws SQLException {
        String sql = "UPDATE folders SET name = ?, color = ?, icon = ?, sort_order = ? WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, folder.getName());
            ps.setString(2, folder.getColor());
            ps.setString(3, folder.getIcon());
            ps.setInt(4, folder.getSortOrder());
            ps.setInt(5, folder.getId());
            ps.setInt(6, SessionContext.getCurrentUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
            "DELETE FROM folders WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, SessionContext.getCurrentUserId());
            ps.executeUpdate();
        }
    }

    private Folder createVirtualFolder(int id, String name, String color, String icon, int taskCount) {
        Folder folder = new Folder(id, name, color, icon, 0, true);
        folder.setTaskCount(taskCount);
        return folder;
    }

    private int getTaskCount(String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tasks WHERE user_id = ? AND " + condition;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Folder mapResultSet(ResultSet rs) throws SQLException {
        Folder folder = new Folder();
        folder.setId(rs.getInt("id"));
        folder.setName(rs.getString("name"));
        folder.setColor(rs.getString("color"));
        folder.setIcon(rs.getString("icon"));
        folder.setParentId(rs.getInt("parent_id"));
        folder.setSortOrder(rs.getInt("sort_order"));
        folder.setDefault(false);
        try {
            folder.setTaskCount(rs.getInt("task_count"));
        } catch (Exception ignored) {
        }
        return folder;
    }
}
