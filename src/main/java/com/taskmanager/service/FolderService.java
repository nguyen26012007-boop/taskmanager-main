package com.taskmanager.service;

import com.taskmanager.dao.FolderDAO;
import com.taskmanager.model.Folder;

import java.sql.SQLException;
import java.util.List;

/**
 * Service layer cho nghiệp vụ Folder.
 */
public class FolderService {

    private final FolderDAO folderDAO = new FolderDAO();

    public List<Folder> getAllFolders() throws SQLException {
        return folderDAO.getAllFolders();
    }

    public Folder getById(int id) throws SQLException {
        return folderDAO.getById(id);
    }

    public int insert(Folder folder) throws SQLException {
        return folderDAO.insert(folder);
    }

    public void update(Folder folder) throws SQLException {
        folderDAO.update(folder);
    }

    public void delete(int id) throws SQLException {
        folderDAO.delete(id);
    }
}
