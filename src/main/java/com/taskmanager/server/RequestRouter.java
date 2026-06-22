package com.taskmanager.server;

import com.taskmanager.model.*;
import com.taskmanager.network.Request;
import com.taskmanager.network.Response;
import com.taskmanager.service.*;
import com.taskmanager.dao.ReminderDAO;
import com.taskmanager.util.SessionContext;

import java.util.List;

/**
 * Điều hướng request từ Client đến các Service tương ứng.
 */
public class RequestRouter {

    private final UserService userService = new UserService();
    private final TaskService taskService = new TaskService();
    private final FolderService folderService = new FolderService();
    private final TagService tagService = new TagService();
    private final ReminderDAO reminderDAO = new ReminderDAO();

    public Response route(Request request, ClientHandler handler) {
        String action = request.getAction();
        try {
            // Các action không cần đăng nhập
            if ("USER_AUTH".equals(action)) {
                String username = request.getString("username");
                String password = request.getString("password");
                User user = userService.authenticate(username, password);
                if (user != null) {
                    handler.setLoggedInUser(user);
                    return Response.ok(user);
                } else {
                    return Response.error("Sai tài khoản hoặc mật khẩu");
                }
            }

            if ("USER_REGISTER".equals(action)) {
                String name = request.getString("name");
                String username = request.getString("username");
                String password = request.getString("password");
                String recoveryPin = request.getString("recoveryPin");
                User user = userService.register(name, username, password, recoveryPin);
                return Response.ok(user);
            }

            if ("USER_RESET_PASSWORD".equals(action)) {
                String username = request.getString("username");
                String recoveryPin = request.getString("recoveryPin");
                String newPassword = request.getString("newPassword");
                boolean success = userService.resetPasswordWithRecoveryPin(username, recoveryPin, newPassword);
                return Response.ok(success);
            }

            // Thiết lập SessionContext cho Thread hiện tại từ handler
            if (handler.getLoggedInUser() != null) {
                SessionContext.setCurrentUser(handler.getLoggedInUser());
            } else {
                return Response.error("Chưa đăng nhập");
            }

            // Các action yêu cầu đăng nhập
            switch (action) {
                // User Actions
                case "USER_GET_CURRENT" -> {
                    User u = userService.getById(SessionContext.getCurrentUserId());
                    handler.setLoggedInUser(u);
                    return Response.ok(u);
                }
                case "USER_UPDATE_PROFILE" -> {
                    String displayName = request.getString("displayName");
                    userService.updateProfile(SessionContext.getCurrentUserId(), displayName);
                    return Response.ok(true);
                }
                case "USER_CHANGE_PASSWORD" -> {
                    String currentPassword = request.getString("currentPassword");
                    String newPassword = request.getString("newPassword");
                    boolean success = userService.changePassword(SessionContext.getCurrentUserId(), currentPassword, newPassword);
                    return Response.ok(success);
                }
                case "USER_UPDATE_RECOVERY_PIN" -> {
                    String currentPassword = request.getString("currentPassword");
                    String newPin = request.getString("newPin");
                    boolean success = userService.updateRecoveryPin(SessionContext.getCurrentUserId(), currentPassword, newPin);
                    return Response.ok(success);
                }
                case "USER_GET_ALL" -> {
                    return Response.ok(userService.getAllRegisteredUsers());
                }
                case "USER_ADMIN_UPDATE" -> {
                    int id = request.getInt("id");
                    String name = request.getString("name");
                    boolean admin = request.getBool("admin");
                    userService.adminUpdateUser(id, name, admin);
                    return Response.ok(true);
                }
                case "USER_ADMIN_RESET_PASSWORD" -> {
                    int id = request.getInt("id");
                    String newPassword = request.getString("newPassword");
                    userService.adminResetPassword(id, newPassword);
                    return Response.ok(true);
                }
                case "USER_ADMIN_RESET_PIN" -> {
                    int id = request.getInt("id");
                    String newPin = request.getString("newPin");
                    userService.adminResetRecoveryPin(id, newPin);
                    return Response.ok(true);
                }
                case "USER_DELETE" -> {
                    int id = request.getInt("id");
                    boolean success = userService.deleteUser(id);
                    return Response.ok(success);
                }
                case "USER_COUNT_ADMINS" -> {
                    return Response.ok(userService.countAdmins());
                }

                // Task Actions
                case "TASK_GET_ALL" -> {
                    Integer folderId = request.getInteger("folderId");
                    String statusFilter = request.getString("statusFilter");
                    String searchText = request.getString("searchText");
                    List<Task> tasks = taskService.getAllTasks(folderId, statusFilter, searchText);
                    return Response.ok(tasks);
                }
                case "TASK_GET_BY_ID" -> {
                    int id = request.getInt("id");
                    return Response.ok(taskService.getById(id));
                }
                case "TASK_INSERT" -> {
                    Task task = (Task) request.get("task");
                    int id = taskService.insert(task);
                    return Response.ok(id);
                }
                case "TASK_UPDATE" -> {
                    Task task = (Task) request.get("task");
                    taskService.update(task);
                    return Response.ok(true);
                }
                case "TASK_DELETE" -> {
                    int id = request.getInt("id");
                    taskService.delete(id);
                    return Response.ok(true);
                }
                case "TASK_DELETE_MULTIPLE" -> {
                    @SuppressWarnings("unchecked")
                    List<Integer> ids = (List<Integer>) request.get("ids");
                    taskService.deleteMultiple(ids);
                    return Response.ok(true);
                }
                case "TASK_UPDATE_STATUS" -> {
                    int id = request.getInt("id");
                    Task.Status status = Task.Status.valueOf(request.getString("status"));
                    taskService.updateStatus(id, status);
                    return Response.ok(true);
                }
                case "TASK_GET_STATS" -> {
                    return Response.ok(taskService.getStatistics());
                }
                case "TASK_GET_COMPLETION_COUNTS" -> {
                    int days = request.getInt("days");
                    return Response.ok(taskService.getCompletionCountsLastDays(days));
                }

                // Folder Actions
                case "FOLDER_GET_ALL" -> {
                    return Response.ok(folderService.getAllFolders());
                }
                case "FOLDER_GET_BY_ID" -> {
                    int id = request.getInt("id");
                    return Response.ok(folderService.getById(id));
                }
                case "FOLDER_INSERT" -> {
                    Folder folder = (Folder) request.get("folder");
                    int id = folderService.insert(folder);
                    return Response.ok(id);
                }
                case "FOLDER_UPDATE" -> {
                    Folder folder = (Folder) request.get("folder");
                    folderService.update(folder);
                    return Response.ok(true);
                }
                case "FOLDER_DELETE" -> {
                    int id = request.getInt("id");
                    folderService.delete(id);
                    return Response.ok(true);
                }

                // Tag Actions
                case "TAG_GET_ALL" -> {
                    return Response.ok(tagService.getAllTags());
                }
                case "TAG_INSERT" -> {
                    Tag tag = (Tag) request.get("tag");
                    return Response.ok(tagService.insert(tag));
                }
                case "TAG_DELETE" -> {
                    int id = request.getInt("id");
                    tagService.delete(id);
                    return Response.ok(true);
                }

                // Reminder Actions
                case "REMINDER_CHECK" -> {
                    List<Reminder> pending = reminderDAO.getPendingReminders();
                    for (Reminder reminder : pending) {
                        if (reminder.getRepeatType() == Reminder.RepeatType.NONE) {
                            reminderDAO.markSent(reminder.getId());
                        } else {
                            reminderDAO.rescheduleNextOccurrence(reminder);
                        }
                    }
                    return Response.ok(pending);
                }
                case "REMINDER_INSERT" -> {
                    Reminder reminder = (Reminder) request.get("reminder");
                    int id = reminderDAO.insert(reminder);
                    return Response.ok(id);
                }
                case "REMINDER_DELETE" -> {
                    int id = request.getInt("id");
                    reminderDAO.delete(id);
                    return Response.ok(true);
                }
                case "REMINDER_GET_BY_TASK" -> {
                    int taskId = request.getInt("taskId");
                    return Response.ok(reminderDAO.getByTaskId(taskId));
                }
                case "REMINDER_GET_UPCOMING" -> {
                    int limit = request.getInt("limit");
                    return Response.ok(reminderDAO.getUpcomingReminders(limit));
                }

                default -> {
                    return Response.error("Hành động không hợp lệ: " + action);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Lỗi hệ thống: " + e.getMessage());
        } finally {
            SessionContext.clear();
        }
    }
}
