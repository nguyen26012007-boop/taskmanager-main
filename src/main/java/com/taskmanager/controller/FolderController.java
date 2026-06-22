package com.taskmanager.controller;

import com.taskmanager.model.Folder;
import com.taskmanager.network.NetworkService;
import com.taskmanager.network.Request;
import com.taskmanager.network.Response;

import java.io.IOException;
import java.util.List;

public class FolderController {

    private final NetworkService network = NetworkService.getInstance();

    public List<Folder> getAllFolders() throws IOException {
        Request req = new Request("FOLDER_GET_ALL");
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public Folder getById(int id) throws IOException {
        Request req = new Request("FOLDER_GET_BY_ID")
                .put("id", id);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public int insert(Folder folder) throws IOException {
        Request req = new Request("FOLDER_INSERT")
                .put("folder", folder);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void update(Folder folder) throws IOException {
        Request req = new Request("FOLDER_UPDATE")
                .put("folder", folder);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void delete(int id) throws IOException {
        Request req = new Request("FOLDER_DELETE")
                .put("id", id);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }
}
