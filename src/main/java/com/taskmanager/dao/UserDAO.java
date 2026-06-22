package com.taskmanager.dao;

import com.taskmanager.model.User;
import com.taskmanager.util.DBConnection;
import com.taskmanager.util.PasswordUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý đăng ký, đăng nhập và quản lý người dùng cục bộ.
 */
public class UserDAO {

    public boolean hasRegisteredUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username IS NOT NULL AND password_hash IS NOT NULL";
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public User register(String name, String username, String rawPassword, String recoveryPin) throws SQLException {
        if (findByUsername(username) != null) {
            throw new SQLException("Tên đăng nhập đã tồn tại");
        }

        boolean makeAdmin = !hasRegisteredUsers();
        String sql = """
            INSERT INTO users (name, username, password_hash, recovery_pin_hash, is_admin, created_at, last_login_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, PasswordUtil.hash(rawPassword));
            ps.setString(4, PasswordUtil.hash(recoveryPin));
            ps.setInt(5, makeAdmin ? 1 : 0);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return getById(keys.getInt(1));
                }
            }
        }
        return null;
    }

    public User authenticate(String username, String rawPassword) throws SQLException {
        String sql = """
            SELECT id, name, username, password_hash, recovery_pin_hash, is_admin, last_login_at
            FROM users
            WHERE username = ? AND password_hash = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(rawPassword));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSet(rs);
                    updateLastLogin(user.getId());
                    user.setLastLoginAt(LocalDateTime.now());
                    return user;
                }
            }
        }
        return null;
    }

    public boolean resetPasswordWithRecoveryPin(String username, String recoveryPin, String newRawPassword) throws SQLException {
        String sql = """
            UPDATE users
            SET password_hash = ?
            WHERE username = ? AND recovery_pin_hash = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newRawPassword));
            ps.setString(2, username);
            ps.setString(3, PasswordUtil.hash(recoveryPin));
            return ps.executeUpdate() > 0;
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = """
            SELECT id, name, username, password_hash, recovery_pin_hash, is_admin, last_login_at
            FROM users
            WHERE username = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapResultSet(rs) : null;
            }
        }
    }

    public User getById(int id) throws SQLException {
        String sql = """
            SELECT id, name, username, password_hash, recovery_pin_hash, is_admin, last_login_at
            FROM users
            WHERE id = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapResultSet(rs) : null;
            }
        }
    }

    public List<User> getAllRegisteredUsers() throws SQLException {
        String sql = """
            SELECT id, name, username, password_hash, recovery_pin_hash, is_admin, last_login_at
            FROM users
            WHERE username IS NOT NULL AND password_hash IS NOT NULL
            ORDER BY is_admin DESC, name COLLATE utf8mb4_unicode_ci, username COLLATE utf8mb4_unicode_ci
        """;
        List<User> users = new ArrayList<>();
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSet(rs));
            }
        }
        return users;
    }

    public void updateProfile(int userId, String displayName) throws SQLException {
        String sql = "UPDATE users SET name = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean changePassword(int userId, String currentRawPassword, String newRawPassword) throws SQLException {
        String sql = """
            UPDATE users
            SET password_hash = ?
            WHERE id = ? AND password_hash = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newRawPassword));
            ps.setInt(2, userId);
            ps.setString(3, PasswordUtil.hash(currentRawPassword));
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateRecoveryPin(int userId, String currentRawPassword, String newRecoveryPin) throws SQLException {
        String sql = """
            UPDATE users
            SET recovery_pin_hash = ?
            WHERE id = ? AND password_hash = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newRecoveryPin));
            ps.setInt(2, userId);
            ps.setString(3, PasswordUtil.hash(currentRawPassword));
            return ps.executeUpdate() > 0;
        }
    }

    public void adminResetRecoveryPin(int userId, String newRecoveryPin) throws SQLException {
        String sql = "UPDATE users SET recovery_pin_hash = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newRecoveryPin));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void adminUpdateUser(int userId, String displayName, boolean admin) throws SQLException {
        String sql = "UPDATE users SET name = ?, is_admin = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setInt(2, admin ? 1 : 0);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void adminResetPassword(int userId, String newRawPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newRawPassword));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public int countAdmins() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE is_admin = 1 AND username IS NOT NULL AND password_hash IS NOT NULL";
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countUsersWithoutRecoveryPin() throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM users
            WHERE username IS NOT NULL
              AND password_hash IS NOT NULL
              AND (recovery_pin_hash IS NULL OR trim(recovery_pin_hash) = '')
        """;
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void updateLastLogin(int id) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection()
            .prepareStatement("UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private User mapResultSet(ResultSet rs) throws SQLException {
        String rawLastLogin = rs.getString("last_login_at");
        LocalDateTime lastLoginAt = rawLastLogin != null && !rawLastLogin.isBlank()
            ? LocalDateTime.parse(rawLastLogin.replace(" ", "T"))
            : null;

        return new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("recovery_pin_hash"),
            rs.getInt("is_admin") == 1,
            lastLoginAt
        );
    }
}
