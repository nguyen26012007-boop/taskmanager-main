package com.taskmanager.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class DBConnection {

    private static final String DB_HOST = config("taskmanager.db.host", "TASKMANAGER_DB_HOST", "127.0.0.1");
    private static final String DB_PORT = config("taskmanager.db.port", "TASKMANAGER_DB_PORT", "3306");
    private static final String DB_NAME = config("taskmanager.db.name", "TASKMANAGER_DB_NAME", "taskmanager");
    private static final String DB_USER = config("taskmanager.db.user", "TASKMANAGER_DB_USER", "root");
    private static final String DB_PASSWORD = config("taskmanager.db.password", "TASKMANAGER_DB_PASSWORD", "");

    private static final String SERVER_URL = "jdbc:mariadb://" + DB_HOST + ":" + DB_PORT
        + "/?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Ho_Chi_Minh";
    private static final String JDBC_URL = "jdbc:mariadb://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Ho_Chi_Minh";

    private static Connection connection;
    private static final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();

    public static void initialize() throws SQLException {
        createDatabaseIfNeeded();
        connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
        createSchema();
        createIndexes();
        migrateSchema();
        insertDefaultData();
        System.out.println("Database initialized at: " + getDatabasePath());
    }

    public static Connection getConnection() {
        try {
            Connection conn = threadConnection.get();
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
                threadConnection.set(conn);
            }
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getDatabasePath() {
        return "mariadb://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    }

    private static String config(String property, String env, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(env);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static void createDatabaseIfNeeded() throws SQLException {
        try (Connection serverConnection = DriverManager.getConnection(SERVER_URL, DB_USER, DB_PASSWORD);
             Statement stmt = serverConnection.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS `" + DB_NAME + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private static void createSchema() throws SQLException {
        String[] sqls = {
            """
            CREATE TABLE IF NOT EXISTS users (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(255) NOT NULL DEFAULT 'Người dùng',
                avatar_path VARCHAR(1024),
                username VARCHAR(255) UNIQUE,
                password_hash VARCHAR(255),
                recovery_pin_hash VARCHAR(255),
                is_admin TINYINT(1) NOT NULL DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                last_login_at DATETIME
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""",
            """
            CREATE TABLE IF NOT EXISTS folders (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(255) NOT NULL,
                color VARCHAR(32) DEFAULT '#E94560',
                icon VARCHAR(64) DEFAULT 'folder',
                parent_id INT,
                sort_order INT DEFAULT 0,
                is_default TINYINT(1) DEFAULT 0,
                user_id INT,
                CONSTRAINT fk_folders_parent FOREIGN KEY (parent_id) REFERENCES folders(id) ON DELETE SET NULL,
                CONSTRAINT fk_folders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""",
            """
            CREATE TABLE IF NOT EXISTS tasks (
                id INT PRIMARY KEY AUTO_INCREMENT,
                title VARCHAR(500) NOT NULL,
                description TEXT,
                folder_id INT,
                priority VARCHAR(16) DEFAULT 'MEDIUM',
                status VARCHAR(20) DEFAULT 'TODO',
                start_date DATE,
                due_date DATE,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                completed_at DATETIME,
                user_id INT,
                CONSTRAINT fk_tasks_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE SET NULL,
                CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                CONSTRAINT chk_tasks_priority CHECK (priority IN ('HIGH','MEDIUM','LOW')),
                CONSTRAINT chk_tasks_status CHECK (status IN ('TODO','IN_PROGRESS','DONE','PAUSED'))
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""",
            """
            CREATE TABLE IF NOT EXISTS subtasks (
                id INT PRIMARY KEY AUTO_INCREMENT,
                task_id INT NOT NULL,
                title VARCHAR(500) NOT NULL,
                is_completed TINYINT(1) DEFAULT 0,
                sort_order INT DEFAULT 0,
                CONSTRAINT fk_subtasks_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""",
            """
            CREATE TABLE IF NOT EXISTS tags (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(255) NOT NULL,
                color VARCHAR(32) DEFAULT '#E94560',
                user_id INT,
                CONSTRAINT fk_tags_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE KEY idx_tags_user_name (user_id, name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""",
            """
            CREATE TABLE IF NOT EXISTS task_tags (
                task_id INT NOT NULL,
                tag_id INT NOT NULL,
                PRIMARY KEY (task_id, tag_id),
                CONSTRAINT fk_task_tags_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                CONSTRAINT fk_task_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""",
            """
            CREATE TABLE IF NOT EXISTS reminders (
                id INT PRIMARY KEY AUTO_INCREMENT,
                task_id INT NOT NULL,
                remind_at DATETIME NOT NULL,
                is_sent TINYINT(1) DEFAULT 0,
                repeat_type VARCHAR(16) DEFAULT 'NONE',
                CONSTRAINT fk_reminders_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                CONSTRAINT chk_reminders_repeat CHECK (repeat_type IN ('NONE','DAILY','WEEKLY'))
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""",
            """
            CREATE TABLE IF NOT EXISTS settings (
                `key` VARCHAR(255) PRIMARY KEY,
                value TEXT
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"""
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : sqls) {
                stmt.execute(sql);
            }
        }
    }

    private static void createIndexes() throws SQLException {
        createIndexIfMissing("tasks", "idx_tasks_folder", "folder_id");
        createIndexIfMissing("tasks", "idx_tasks_status", "status");
        createIndexIfMissing("tasks", "idx_tasks_due_date", "due_date");
        createIndexIfMissing("tasks", "idx_tasks_user", "user_id");
        createIndexIfMissing("subtasks", "idx_subtasks_task", "task_id");
        createIndexIfMissing("reminders", "idx_reminders_remind_at", "remind_at, is_sent");
        createIndexIfMissing("folders", "idx_folders_user", "user_id");
        createIndexIfMissing("tags", "idx_tags_user", "user_id");
    }

    private static void createIndexIfMissing(String tableName, String indexName, String columns) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return;
                }
            }
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX " + indexName + " ON " + tableName + " (" + columns + ")");
        }
    }

    private static void migrateSchema() throws SQLException {
        Set<String> userColumns = getColumns("users");
        Set<String> folderColumns = getColumns("folders");
        Set<String> taskColumns = getColumns("tasks");
        Set<String> tagColumns = getColumns("tags");

        try (Statement stmt = connection.createStatement()) {
            if (!userColumns.contains("username")) {
                stmt.execute("ALTER TABLE users ADD COLUMN username VARCHAR(255) UNIQUE");
            }
            if (!userColumns.contains("password_hash")) {
                stmt.execute("ALTER TABLE users ADD COLUMN password_hash VARCHAR(255)");
            }
            if (!userColumns.contains("last_login_at")) {
                stmt.execute("ALTER TABLE users ADD COLUMN last_login_at DATETIME");
            }
            if (!userColumns.contains("is_admin")) {
                stmt.execute("ALTER TABLE users ADD COLUMN is_admin TINYINT(1) NOT NULL DEFAULT 0");
            }
            if (!userColumns.contains("recovery_pin_hash")) {
                stmt.execute("ALTER TABLE users ADD COLUMN recovery_pin_hash VARCHAR(255)");
            }

            if (!folderColumns.contains("user_id")) {
                stmt.execute("ALTER TABLE folders ADD COLUMN user_id INT");
            }
            if (!taskColumns.contains("user_id")) {
                stmt.execute("ALTER TABLE tasks ADD COLUMN user_id INT");
            }
            if (!tagColumns.contains("user_id")) {
                stmt.execute("ALTER TABLE tags ADD COLUMN user_id INT");
            }

            if (!hasAdmin(stmt)) {
                int firstRegisteredUserId = findFirstRegisteredUserId(stmt);
                if (firstRegisteredUserId > 0) {
                    stmt.execute("UPDATE users SET is_admin = 1 WHERE id = " + firstRegisteredUserId);
                }
            }

            int fallbackUserId = findFallbackUserId(stmt);
            stmt.execute("UPDATE tasks SET user_id = " + fallbackUserId + " WHERE user_id IS NULL");
            stmt.execute("UPDATE folders SET user_id = " + fallbackUserId + " WHERE user_id IS NULL AND is_default = 0");
            stmt.execute("UPDATE tags SET user_id = " + fallbackUserId + " WHERE user_id IS NULL");
        }
    }

    private static int findFallbackUserId(Statement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("""
            SELECT id
            FROM users
            WHERE username IS NOT NULL OR id = 1
            ORDER BY id
            LIMIT 1
        """)) {
            return rs.next() ? rs.getInt("id") : 1;
        }
    }

    private static boolean hasAdmin(Statement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM users WHERE is_admin = 1 LIMIT 1")) {
            return rs.next();
        }
    }

    private static int findFirstRegisteredUserId(Statement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("""
            SELECT id
            FROM users
            WHERE username IS NOT NULL AND password_hash IS NOT NULL
            ORDER BY id
            LIMIT 1
        """)) {
            return rs.next() ? rs.getInt("id") : -1;
        }
    }

    private static Set<String> getColumns(String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    private static void insertDefaultData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                INSERT IGNORE INTO users (id, name) VALUES (1, 'Người dùng')
            """);

            stmt.execute("""
                INSERT IGNORE INTO settings (`key`, value) VALUES
                ('theme', 'light'),
                ('window_width', '1200'),
                ('window_height', '750'),
                ('sound_enabled', 'true'),
                ('last_view', 'dashboard'),
                ('remind_before_minutes', '15')
            """);
        }
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            Connection conn = threadConnection.get();
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
            threadConnection.remove();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
