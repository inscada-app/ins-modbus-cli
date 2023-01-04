package com.inscada.modbus.client.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inscada.modbus.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class ConfigService {
    private static final Logger log = LoggerFactory.getLogger("Config Service");
    private final ObjectMapper objectMapper;
    private final ConnectionService connectionService;
    private final DataBlockService dataBlockService;

    public ConfigService(ConnectionService connectionService, DataBlockService dataBlockService) {
        this.connectionService = connectionService;
        this.dataBlockService = dataBlockService;
        this.objectMapper = new ObjectMapper();
    }
    public Result<Void> save(File saveFile) {
        try {
            Config config = new Config(connectionService.getConnection(), dataBlockService.getDataBlocks(),
                    dataBlockService.getTags());
            objectMapper.writeValue(saveFile, config);
            log.info("Config saved into " + saveFile.getName());
            return Result.createSuccessResult();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.createErrorResult(List.of(e.getMessage()));
        }
    }

    public Result<Void> load(File loadFile) {
        if (connectionService.isConnected()) {
            return Result.createErrorResult(List.of("Cannot load while connection is running"));
        }
        try {
            Config config = objectMapper.readValue(loadFile, Config.class);
            log.info("Config read from " + loadFile.getName());
            connectionService.setConnection(config.getConnection());
            dataBlockService.loadDataBlocks(config.getDataBlocks());
            dataBlockService.loadTags(config.getTags());
            return Result.createSuccessResult();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.createErrorResult(List.of(e.getMessage()));
        }
    }


}
