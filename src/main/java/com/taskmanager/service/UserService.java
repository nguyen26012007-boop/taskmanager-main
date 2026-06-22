package com.taskmanager.service;

import com.taskmanager.dao.UserDAO;
import com.taskmanager.model.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Service layer cho nghiệp vụ User.
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();

    public User authenticate(String username, String rawPassword) throws SQLException {
        return userDAO.authenticate(username, rawPassword);
    }

    public User register(String name, String username, String rawPassword, String recoveryPin) throws SQLException {
        return userDAO.register(name, username, rawPassword, recoveryPin);
    }

    public boolean resetPasswordWithRecoveryPin(String username, String recoveryPin, String newRawPassword) throws SQLException {
        return userDAO.resetPasswordWithRecoveryPin(username, recoveryPin, newRawPassword);
    }

    public void updateProfile(int userId, String displayName) throws SQLException {
        userDAO.updateProfile(userId, displayName);
    }

    public boolean changePassword(int userId, String currentRawPassword, String newRawPassword) throws SQLException {
        return userDAO.changePassword(userId, currentRawPassword, newRawPassword);
    }

    public boolean updateRecoveryPin(int userId, String currentRawPassword, String newRecoveryPin) throws SQLException {
        return userDAO.updateRecoveryPin(userId, currentRawPassword, newRecoveryPin);
    }

    public void adminResetRecoveryPin(int userId, String newRecoveryPin) throws SQLException {
        userDAO.adminResetRecoveryPin(userId, newRecoveryPin);
    }

    public void adminUpdateUser(int userId, String displayName, boolean admin) throws SQLException {
        userDAO.adminUpdateUser(userId, displayName, admin);
    }

    public void adminResetPassword(int userId, String newRawPassword) throws SQLException {
        userDAO.adminResetPassword(userId, newRawPassword);
    }

    public boolean deleteUser(int userId) throws SQLException {
        return userDAO.deleteUser(userId);
    }

    public List<User> getAllRegisteredUsers() throws SQLException {
        return userDAO.getAllRegisteredUsers();
    }

    public int countAdmins() throws SQLException {
        return userDAO.countAdmins();
    }

    public User getById(int id) throws SQLException {
        return userDAO.getById(id);
    }
}
