package com.taskmanager.controller;

import com.taskmanager.model.User;
import com.taskmanager.network.NetworkService;
import com.taskmanager.network.Request;
import com.taskmanager.network.Response;
import com.taskmanager.util.SessionContext;

import java.io.IOException;
import java.util.List;

public class UserController {

    private final NetworkService network = NetworkService.getInstance();

    public User login(String username, String password) throws IOException {
        Request req = new Request("USER_AUTH")
                .put("username", username)
                .put("password", password);
        Response res = network.send(req);
        if (res.isSuccess()) {
            User user = res.getDataAs();
            SessionContext.setCurrentUser(user);
            return user;
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public User register(String name, String username, String password, String recoveryPin) throws IOException {
        Request req = new Request("USER_REGISTER")
                .put("name", name)
                .put("username", username)
                .put("password", password)
                .put("recoveryPin", recoveryPin);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public boolean resetPassword(String username, String recoveryPin, String newPassword) throws IOException {
        Request req = new Request("USER_RESET_PASSWORD")
                .put("username", username)
                .put("recoveryPin", recoveryPin)
                .put("newPassword", newPassword);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void updateProfile(String displayName) throws IOException {
        Request req = new Request("USER_UPDATE_PROFILE")
                .put("displayName", displayName);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
        // Refresh local SessionContext
        refreshCurrentUser();
    }

    public boolean changePassword(String currentPassword, String newPassword) throws IOException {
        Request req = new Request("USER_CHANGE_PASSWORD")
                .put("currentPassword", currentPassword)
                .put("newPassword", newPassword);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public boolean updateRecoveryPin(String currentPassword, String newPin) throws IOException {
        Request req = new Request("USER_UPDATE_RECOVERY_PIN")
                .put("currentPassword", currentPassword)
                .put("newPin", newPin);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public List<User> getAllUsers() throws IOException {
        Request req = new Request("USER_GET_ALL");
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void adminUpdateUser(int id, String name, boolean admin) throws IOException {
        Request req = new Request("USER_ADMIN_UPDATE")
                .put("id", id)
                .put("name", name)
                .put("admin", admin);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void adminResetPassword(int id, String newPassword) throws IOException {
        Request req = new Request("USER_ADMIN_RESET_PASSWORD")
                .put("id", id)
                .put("newPassword", newPassword);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void adminResetPin(int id, String newPin) throws IOException {
        Request req = new Request("USER_ADMIN_RESET_PIN")
                .put("id", id)
                .put("newPin", newPin);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }

    public boolean deleteUser(int id) throws IOException {
        Request req = new Request("USER_DELETE")
                .put("id", id);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public int countAdmins() throws IOException {
        Request req = new Request("USER_COUNT_ADMINS");
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void refreshCurrentUser() {
        Request req = new Request("USER_GET_CURRENT");
        Response res = network.send(req);
        if (res.isSuccess() && res.getData() != null) {
            SessionContext.setCurrentUser(res.getDataAs());
        }
    }
}
