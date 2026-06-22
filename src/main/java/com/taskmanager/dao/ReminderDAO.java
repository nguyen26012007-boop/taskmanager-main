package com.taskmanager.dao;

import com.taskmanager.model.Reminder;
import com.taskmanager.util.DBConnection;
import com.taskmanager.util.SessionContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReminderDAO {

    public List<Reminder> getPendingReminders() throws SQLException {
        List<Reminder> reminders = new ArrayList<>();
        String sql = """
            SELECT r.*, t.title AS task_title
            FROM reminders r
            JOIN tasks t ON r.task_id = t.id
            WHERE r.is_sent = 0
              AND r.remind_at <= DATE_ADD(NOW(), INTERVAL 1 MINUTE)
              AND t.user_id = ?
            ORDER BY r.remind_at
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reminders.add(mapReminder(rs, true));
            }
        }
        return reminders;
    }

    public List<Reminder> getUpcomingReminders(int limit) throws SQLException {
        List<Reminder> reminders = new ArrayList<>();
        String sql = """
            SELECT r.*, t.title AS task_title
            FROM reminders r
            JOIN tasks t ON r.task_id = t.id
            WHERE t.user_id = ?
              AND r.remind_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
            ORDER BY r.remind_at
            LIMIT ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, SessionContext.getCurrentUserId());
            ps.setInt(2, Math.max(limit, 1));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reminders.add(mapReminder(rs, true));
            }
        }
        return reminders;
    }

    public int insert(Reminder reminder) throws SQLException {
        String sql = "INSERT INTO reminders (task_id, remind_at, repeat_type) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reminder.getTaskId());
            ps.setString(2, reminder.getRemindAt().toString().replace("T", " "));
            ps.setString(3, reminder.getRepeatType().name());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        return -1;
    }

    public void markSent(int id) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement("UPDATE reminders SET is_sent = 1 WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void rescheduleNextOccurrence(Reminder reminder) throws SQLException {
        LocalDateTime nextTime = switch (reminder.getRepeatType()) {
            case DAILY -> reminder.getRemindAt().plusDays(1);
            case WEEKLY -> reminder.getRemindAt().plusWeeks(1);
            case NONE -> null;
        };
        if (nextTime == null) {
            markSent(reminder.getId());
            return;
        }

        String sql = "UPDATE reminders SET remind_at = ?, is_sent = 0 WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, nextTime.toString().replace("T", " "));
            ps.setInt(2, reminder.getId());
            ps.executeUpdate();
        }
    }

    public List<Reminder> getByTaskId(int taskId) throws SQLException {
        List<Reminder> reminders = new ArrayList<>();
        String sql = """
            SELECT r.*, t.title AS task_title
            FROM reminders r
            JOIN tasks t ON r.task_id = t.id
            WHERE r.task_id = ? AND t.user_id = ?
            ORDER BY r.remind_at
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, SessionContext.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reminders.add(mapReminder(rs, true));
            }
        }
        return reminders;
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement("DELETE FROM reminders WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Reminder mapReminder(ResultSet rs, boolean withTaskTitle) throws SQLException {
        Reminder reminder = new Reminder();
        reminder.setId(rs.getInt("id"));
        reminder.setTaskId(rs.getInt("task_id"));
        if (withTaskTitle) {
            reminder.setTaskTitle(rs.getString("task_title"));
        }
        String remindAt = rs.getString("remind_at");
        if (remindAt != null) {
            reminder.setRemindAt(LocalDateTime.parse(remindAt.replace(" ", "T")));
        }
        reminder.setSent(rs.getInt("is_sent") == 1);
        String repeatType = rs.getString("repeat_type");
        reminder.setRepeatType(repeatType != null ? Reminder.RepeatType.valueOf(repeatType) : Reminder.RepeatType.NONE);
        return reminder;
    }
}
