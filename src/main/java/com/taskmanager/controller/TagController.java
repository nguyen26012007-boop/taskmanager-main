package com.taskmanager.controller;

import com.taskmanager.model.Tag;
import com.taskmanager.network.NetworkService;
import com.taskmanager.network.Request;
import com.taskmanager.network.Response;

import java.io.IOException;
import java.util.List;

public class TagController {

    private final NetworkService network = NetworkService.getInstance();

    public List<Tag> getAllTags() throws IOException {
        Request req = new Request("TAG_GET_ALL");
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public Tag insert(Tag tag) throws IOException {
        Request req = new Request("TAG_INSERT")
                .put("tag", tag);
        Response res = network.send(req);
        if (res.isSuccess()) {
            return res.getDataAs();
        } else {
            throw new IOException(res.getErrorMessage());
        }
    }

    public void delete(int id) throws IOException {
        Request req = new Request("TAG_DELETE")
                .put("id", id);
        Response res = network.send(req);
        if (!res.isSuccess()) {
            throw new IOException(res.getErrorMessage());
        }
    }
}
