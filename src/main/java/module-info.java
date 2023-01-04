module com.inscada.modbus.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires j2mod;
    requires slf4j.api;
    requires logback.classic;
    requires com.fasterxml.jackson.databind;
    requires org.jfree.jfreechart;
    requires org.jfree.chart.fx;

    opens com.inscada.modbus.client;
    opens com.inscada.modbus.client.utils;
    opens com.inscada.modbus.client.model;
    opens com.inscada.modbus.client.services;
    opens com.inscada.modbus.client.controllers;
}