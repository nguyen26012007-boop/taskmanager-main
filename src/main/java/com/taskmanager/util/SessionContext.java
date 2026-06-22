package com.taskmanager.util;

import com.taskmanager.model.User;

public final class SessionContext {

    private static final ThreadLocal<User> currentUser = new InheritableThreadLocal<>();

    private SessionContext() {
    }

    public static void setCurrentUser(User user) {
        currentUser.set(user);
    }

    public static User getCurrentUser() {
        return currentUser.get();
    }

    public static int getCurrentUserId() {
        User user = currentUser.get();
        return user != null ? user.getId() : -1;
    }

    public static boolean hasCurrentUser() {
        return currentUser.get() != null;
    }

    public static void clear() {
        currentUser.remove();
    }
}
