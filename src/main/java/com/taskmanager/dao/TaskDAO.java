package com.taskmanager.dao;

import com.taskmanager.model.Subtask;
import com.taskmanager.model.Tag;
import com.taskmanager.model.Task;
import com.taskmanager.util.DBConnection;
import com.taskmanager.util.SessionContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TaskDAO {

    public List<Task> getAllTasks(Integer folderId, String statusFilter, String searchText) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT t.*, f.name AS folder_name
            FROM tasks t
            LEFT JOIN folders f ON t.folder_id = f.id
            WHERE t.user_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(SessionContext.getCurrentUserId());

        if (folderId != null) {
            if (folderId == FolderDAO.ALL_FOLDER_ID) {
                // no extra filter
            } else if (folderId == FolderDAO.TODAY_FOLDER_ID) {
                sql.append(" AND DATE(t.due_date) = CURDATE() AND t.status != 'DONE'");
            } else if (folderId == FolderDAO.IMPORTANT_FOLDER_ID) {
                sql.append(" AND t.priority = 'HIGH' AND t.status != 'DONE'");
            } else if (folderId == FolderDAO.DONE_FOLDER_ID) {
                sql.append(" AND t.status = 'DONE'");
            } else if (folderId > 0) {
                sql.append(" AND t.folder_id = ?");
                params.add(folderId);
            }
        }

        if (statusFilter != null && !statusFilter.isBlank()) {
            sql.append(" AND t.status = ?");
            params.add(statusFilter);
        }

        if (searchText != null && !searchText.isBlank()) {
            sql.append(" AND (lower(t.title) LIKE ? OR lower(coalesce(t.description, '')) LIKE ?)");
            String search = "%" + searchText.toLowerCase() + "%";
            params.add(search);
            params.add(search);
        }

        sql.append("""
            ORDER BY
                CASE t.priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END,
                CASE WHEN t.due_date IS NULL THEN 1 ELSE 0 END,
                t.due_date ASC,
                t.created_at DESC
        """);

        List<Task> tasks = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(mapResultSet(rs));
            }
        }

        for (Task task : tasks) {
            task.setSubtasks(getSubtasks(task.getId()));
            task.setTags(getTags(task.getId()));
        }
        return tasks;
    }

    public Task getById(int id) throws SQLException {
        String sql = """
            SELECT t.*, f.name AS folder_name
            FROM tasks t
            LEFT JOIN folders f ON t.folder_id = f.id
            WHERE t.id = ? AND t.user_id = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Task task = mapResultSet(rs);
                task.setSubtasks(getSubtasks(id));
                task.setTags(getTags(id));
                return task;
            }
        }
        return null;
    }

    public int insert(Task task) throws SQLException {
        String sql = """
            INSERT INTO tasks (title, description, folder_id, priority, status, start_date, due_date, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setObject(3, task.getFolderId() > 0 ? task.getFolderId() : null);
            ps.setString(4, task.getPriority().name());
            ps.setString(5, task.getStatus().name());
            ps.setObject(6, task.getStartDate() != null ? task.getStartDate().toString() : null);
            ps.setObject(7, task.getDueDate() != null ? task.getDueDate().toString() : null);
            ps.setInt(8, SessionContext.getCurrentUserId());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int newId = keys.getInt(1);
                task.setId(newId);
                saveSubtasks(newId, task.getSubtasks());
                saveTags(newId, task.getTags());
                return newId;
            }
        }
        return -1;
    }

    public void update(Task task) throws SQLException {
        String sql = """
            UPDATE tasks
            SET title = ?, description = ?, folder_id = ?, priority = ?, status = ?,
                start_date = ?, due_date = ?, updated_at = CURRENT_TIMESTAMP,
                completed_at = CASE
                    WHEN ? = 'DONE' AND completed_at IS NULL THEN CURRENT_TIMESTAMP
                    WHEN ? != 'DONE' THEN NULL
                    ELSE completed_at
                END
            WHERE id = ? AND user_id = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setObject(3, task.getFolderId() > 0 ? task.getFolderId() : null);
            ps.setString(4, task.getPriority().name());
            ps.setString(5, task.getStatus().name());
            ps.setObject(6, task.getStartDate() != null ? task.getStartDate().toString() : null);
            ps.setObject(7, task.getDueDate() != null ? task.getDueDate().toString() : null);
            ps.setString(8, task.getStatus().name());
            ps.setString(9, task.getStatus().name());
            ps.setInt(10, task.getId());
            ps.setInt(11, SessionContext.getCurrentUserId());
            ps.executeUpdate();
        }

        deleteSubtasks(task.getId());
        saveSubtasks(task.getId(), task.getSubtasks());
        deleteTaskTags(task.getId());
        saveTags(task.getId(), task.getTags());
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
            "DELETE FROM tasks WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, SessionContext.getCurrentUserId());
            ps.executeUpdate();
        }
    }

    public void deleteMultiple(List<Integer> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        String sql = "DELETE FROM tasks WHERE user_id = ? AND id IN (" + "?,".repeat(ids.size()).replaceAll(",$", "") + ")";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, SessionContext.getCurrentUserId());
            for (int i = 0; i < ids.size(); i++) {
                ps.setInt(i + 2, ids.get(i));
            }
            ps.executeUpdate();
        }
    }

    public void updateStatus(int taskId, Task.Status status) throws SQLException {
        String sql = """
            UPDATE tasks
            SET status = ?, updated_at = CURRENT_TIMESTAMP,
                completed_at = CASE WHEN ? = 'DONE' THEN CURRENT_TIMESTAMP ELSE NULL END
            WHERE id = ? AND user_id = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, status.name());
            ps.setInt(3, taskId);
            ps.setInt(4, SessionContext.getCurrentUserId());
            ps.executeUpdate();
        }
    }

    public int[] getStatistics() throws SQLException {
        int[] stats = new int[5];
        String sql = """
            SELECT
                COUNT(*) AS total,
                SUM(CASE WHEN status='TODO' THEN 1 ELSE 0 END) AS todo,
                SUM(CASE WHEN status='IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress,
                SUM(CASE WHEN status='DONE' THEN 1 ELSE 0 END) AS done,
                SUM(CASE WHEN due_date < CURDATE() AND status!='DONE' THEN 1 ELSE 0 END) AS overdue
            FROM tasks
            WHERE user_id = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                stats[0] = rs.getInt("total");
                stats[1] = rs.getInt("todo");
                stats[2] = rs.getInt("in_progress");
                stats[3] = rs.getInt("done");
                stats[4] = rs.getInt("overdue");
            }
        }
        return stats;
    }

    public Map<LocalDate, Long> getCompletionCountsLastDays(int days) throws SQLException {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            counts.put(start.plusDays(i), 0L);
        }

        String sql = """
            SELECT date(completed_at) AS day, COUNT(*) AS count
            FROM tasks
            WHERE completed_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
              AND status='DONE'
              AND user_id = ?
            GROUP BY date(completed_at)
            ORDER BY day
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, days - 1);
            ps.setInt(2, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LocalDate day = LocalDate.parse(rs.getString("day"));
                if (counts.containsKey(day)) {
                    counts.put(day, rs.getLong("count"));
                }
            }
        }
        return counts;
    }

    private Task mapResultSet(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getInt("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setFolderId(rs.getInt("folder_id"));
        task.setFolderName(rs.getString("folder_name"));

        String priority = rs.getString("priority");
        task.setPriority(priority != null ? Task.Priority.valueOf(priority) : Task.Priority.MEDIUM);

        String status = rs.getString("status");
        task.setStatus(status != null ? Task.Status.valueOf(status) : Task.Status.TODO);

        String startDate = rs.getString("start_date");
        if (startDate != null) {
            task.setStartDate(LocalDate.parse(startDate));
        }

        String dueDate = rs.getString("due_date");
        if (dueDate != null) {
            task.setDueDate(LocalDate.parse(dueDate));
        }

        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            task.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        }

        String updatedAt = rs.getString("updated_at");
        if (updatedAt != null) {
            task.setUpdatedAt(LocalDateTime.parse(updatedAt.replace(" ", "T")));
        }

        String completedAt = rs.getString("completed_at");
        if (completedAt != null) {
            task.setCompletedAt(LocalDateTime.parse(completedAt.replace(" ", "T")));
        }

        return task;
    }

    private List<Subtask> getSubtasks(int taskId) throws SQLException {
        List<Subtask> subtasks = new ArrayList<>();
        String sql = """
            SELECT s.*
            FROM subtasks s
            JOIN tasks t ON s.task_id = t.id
            WHERE s.task_id = ? AND t.user_id = ?
            ORDER BY s.sort_order
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                subtasks.add(new Subtask(
                    rs.getInt("id"),
                    rs.getInt("task_id"),
                    rs.getString("title"),
                    rs.getInt("is_completed") == 1,
                    rs.getInt("sort_order")
                ));
            }
        }
        return subtasks;
    }

    private List<Tag> getTags(int taskId) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String sql = """
            SELECT tg.*
            FROM tags tg
            JOIN task_tags tt ON tg.id = tt.tag_id
            JOIN tasks t ON tt.task_id = t.id
            WHERE tt.task_id = ? AND t.user_id = ?
            ORDER BY tg.name
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tags.add(new Tag(rs.getInt("id"), rs.getString("name"), rs.getString("color")));
            }
        }
        return tags;
    }

    private void saveSubtasks(int taskId, List<Subtask> subtasks) throws SQLException {
        if (subtasks == null || subtasks.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO subtasks (task_id, title, is_completed, sort_order) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < subtasks.size(); i++) {
                Subtask subtask = subtasks.get(i);
                ps.setInt(1, taskId);
                ps.setString(2, subtask.getTitle());
                ps.setInt(3, subtask.isCompleted() ? 1 : 0);
                ps.setInt(4, i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveTags(int taskId, List<Tag> tags) throws SQLException {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        String sql = "INSERT IGNORE INTO task_tags (task_id, tag_id) VALUES (?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            for (Tag tag : tags) {
                ps.setInt(1, taskId);
                ps.setInt(2, tag.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deleteSubtasks(int taskId) throws SQLException {
        String sql = """
            DELETE FROM subtasks
            WHERE task_id = ?
              AND EXISTS (SELECT 1 FROM tasks t WHERE t.id = ? AND t.user_id = ?)
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, taskId);
            ps.setInt(3, SessionContext.getCurrentUserId());
            ps.executeUpdate();
        }
    }

    private void deleteTaskTags(int taskId) throws SQLException {
        String sql = """
            DELETE FROM task_tags
            WHERE task_id = ?
              AND EXISTS (SELECT 1 FROM tasks t WHERE t.id = ? AND t.user_id = ?)
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, taskId);
            ps.setInt(3, SessionContext.getCurrentUserId());
            ps.executeUpdate();
        }
    }
}
