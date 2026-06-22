package com.taskmanager.controller;

import com.taskmanager.model.Reminder;
import com.taskmanager.network.NetworkService;
import com.taskmanager.network.Request;
import com.taskmanager.network.Response;

import java.io.IOException;
import java.util.List;

public class ReminderController {

    private final NetworkService network = NetworkService.getInstance();

    public List<Reminder> checkPendingReminders() throws IOException {
        Request req = new Request("REMINDER_CHECK");
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public int insert(Reminder reminder) throws IOException {
        Request req = new Request("REMINDER_INSERT")
                .put("reminder", reminder);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void delete(int id) throws IOException {
        Request req = new Request("REMINDER_DELETE")
                .put("id", id);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }

    public List<Reminder> getByTaskId(int taskId) throws IOException {
        Request req = new Request("REMINDER_GET_BY_TASK")
                .put("taskId", taskId);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public List<Reminder> getUpcomingReminders(int limit) throws IOException {
        Request req = new Request("REMINDER_GET_UPCOMING")
                .put("limit", limit);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }
}
