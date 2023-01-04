package com.inscada.modbus.client.model;

import java.util.List;

public class Config {
    private Connection connection;
    private List<DataBlock> dataBlocks;

    private List<Tag> tags;

    public Config() {
    }
    public Config(Connection connection, List<DataBlock> dataBlocks, List<Tag> tags) {
        this.connection = connection;
        this.dataBlocks = dataBlocks;
        this.tags = tags;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public List<DataBlock> getDataBlocks() {
        return dataBlocks;
    }

    public void setDataBlocks(List<DataBlock> dataBlocks) {
        this.dataBlocks = dataBlocks;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}