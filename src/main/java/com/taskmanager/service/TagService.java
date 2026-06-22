package com.taskmanager.service;

import com.taskmanager.dao.TagDAO;
import com.taskmanager.model.Tag;

import java.sql.SQLException;
import java.util.List;

/**
 * Service layer cho nghiệp vụ Tag.
 */
public class TagService {

    private final TagDAO tagDAO = new TagDAO();

    public List<Tag> getAllTags() throws SQLException {
        return tagDAO.getAllTags();
    }

    public Tag insert(Tag tag) throws SQLException {
        return tagDAO.insert(tag);
    }

    public void delete(int id) throws SQLException {
        tagDAO.delete(id);
    }
}
