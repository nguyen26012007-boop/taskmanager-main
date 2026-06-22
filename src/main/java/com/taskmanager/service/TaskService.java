package com.taskmanager.service;

import com.taskmanager.dao.TaskDAO;
import com.taskmanager.model.Task;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service layer cho nghiệp vụ Task.
 * Đứng giữa Controller/RequestRouter và DAO.
 */
public class TaskService {

    private final TaskDAO taskDAO = new TaskDAO();

    public List<Task> getAllTasks(Integer folderId, String statusFilter, String searchText) throws SQLException {
        return taskDAO.getAllTasks(folderId, statusFilter, searchText);
    }

    public Task getById(int id) throws SQLException {
        return taskDAO.getById(id);
    }

    public int insert(Task task) throws SQLException {
        return taskDAO.insert(task);
    }

    public void update(Task task) throws SQLException {
        taskDAO.update(task);
    }

    public void delete(int id) throws SQLException {
        taskDAO.delete(id);
    }

    public void deleteMultiple(List<Integer> ids) throws SQLException {
        taskDAO.deleteMultiple(ids);
    }

    public void updateStatus(int taskId, Task.Status status) throws SQLException {
        taskDAO.updateStatus(taskId, status);
    }

    public int[] getStatistics() throws SQLException {
        return taskDAO.getStatistics();
    }

    public Map<LocalDate, Long> getCompletionCountsLastDays(int days) throws SQLException {
        return taskDAO.getCompletionCountsLastDays(days);
    }
}
