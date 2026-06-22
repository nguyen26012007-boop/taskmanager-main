module com.taskmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.media;
    requires javafx.web;

    requires java.sql;
    requires java.desktop;

    requires org.mariadb.jdbc;

    requires org.apache.pdfbox;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;

    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.ikonli.material2;

    opens com.taskmanager to javafx.fxml;
    opens com.taskmanager.model to javafx.base;
    opens com.taskmanager.view to javafx.fxml;

    exports com.taskmanager;
    exports com.taskmanager.model;
    exports com.taskmanager.dao;
    exports com.taskmanager.service;
    exports com.taskmanager.util;
    exports com.taskmanager.view;
    exports com.taskmanager.controller;
    exports com.taskmanager.network;
    exports com.taskmanager.server;
}
