package com.taskmanager.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model đại diện cho tài khoản người dùng.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private String username;
    private String passwordHash;
    private String recoveryPinHash;
    private boolean admin;
    private LocalDateTime lastLoginAt;

    public User() {}

    public User(int id, String name, String username, String passwordHash, String recoveryPinHash, boolean admin) {
        this(id, name, username, passwordHash, recoveryPinHash, admin, null);
    }

    public User(int id, String name, String username, String passwordHash, String recoveryPinHash, boolean admin, LocalDateTime lastLoginAt) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.passwordHash = passwordHash;
        this.recoveryPinHash = recoveryPinHash;
        this.admin = admin;
        this.lastLoginAt = lastLoginAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRecoveryPinHash() { return recoveryPinHash; }
    public void setRecoveryPinHash(String recoveryPinHash) { this.recoveryPinHash = recoveryPinHash; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getDisplayName() {
        return name != null && !name.isBlank() ? name : username;
    }

    public boolean hasRecoveryPin() {
        return recoveryPinHash != null && !recoveryPinHash.isBlank();
    }
}
