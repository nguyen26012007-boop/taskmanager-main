package com.taskmanager.controller;

import com.taskmanager.model.Task;
import com.taskmanager.network.NetworkService;
import com.taskmanager.network.Request;
import com.taskmanager.network.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class TaskController {

    private final NetworkService network = NetworkService.getInstance();

    public List<Task> getAllTasks(Integer folderId, String statusFilter, String searchText) throws IOException {
        Request req = new Request("TASK_GET_ALL")
                .put("folderId", folderId)
                .put("statusFilter", statusFilter)
                .put("searchText", searchText);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public Task getById(int id) throws IOException {
        Request req = new Request("TASK_GET_BY_ID")
                .put("id", id);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public int insert(Task task) throws IOException {
        Request req = new Request("TASK_INSERT")
                .put("task", task);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void update(Task task) throws IOException {
        Request req = new Request("TASK_UPDATE")
                .put("task", task);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void delete(int id) throws IOException {
        Request req = new Request("TASK_DELETE")
                .put("id", id);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void deleteMultiple(List<Integer> ids) throws IOException {
        Request req = new Request("TASK_DELETE_MULTIPLE")
                .put("ids", ids);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void updateStatus(int taskId, Task.Status status) throws IOException {
        Request req = new Request("TASK_UPDATE_STATUS")
                .put("id", taskId)
                .put("status", status.name());
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }

    public int[] getStatistics() throws IOException {
        Request req = new Request("TASK_GET_STATS");
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public Map<LocalDate, Long> getCompletionCountsLastDays(int days) throws IOException {
        Request req = new Request("TASK_GET_COMPLETION_COUNTS")
                .put("days", days);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }
}
