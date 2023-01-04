package com.inscada.modbus.client.services;

import com.inscada.modbus.client.model.Connection;
import com.inscada.modbus.client.model.ConnectionType;
import com.inscada.modbus.client.model.Result;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class ConnectionService {

    private static final int PERIOD = 2000;
    private static final int INITIAL_DELAY = 0;
    private final SimpleObjectProperty<Connection> connection;
    private Timer statusTimer;
    private volatile InsModbusMaster modbusMaster;
    private volatile Consumer<Boolean> statusConsumer;

    public ConnectionService() {
        this.connection = new SimpleObjectProperty<>(new Connection());
    }

    public Result<Void> connect() {
        Connection activeConnection = connection.get();
        List<String> errors = new ArrayList<>();
        if (!validateConnection(activeConnection, errors)) {
            return Result.createErrorResult(errors);
        }
        disconnect();
        boolean rtuOverTCP = activeConnection.getConnectionType().equals(ConnectionType.ModbusRtuOverTcp);
        boolean isUDPSelected = activeConnection.getConnectionType().equals(ConnectionType.ModbusUDP);
        try {
            if (isUDPSelected) {
                modbusMaster = new InsModbusUDPMaster(activeConnection.getIpAddress(), activeConnection.getPort(),
                        activeConnection.getTimeout());
            } else {
                modbusMaster = new InsModbusTCPMaster(activeConnection.getIpAddress(), activeConnection.getPort(),
                        activeConnection.getTimeout(), false, rtuOverTCP);
            }
            modbusMaster.connect();
            statusTimer = new Timer("Connection Status Timer", true);
            statusTimer.schedule(getStatusTimerTask(), INITIAL_DELAY, PERIOD);
            return Result.createSuccessResult();
        } catch (Exception e) {
            errors.add(e.getMessage());
            return Result.createErrorResult(errors);
        }

    }

    public void disconnect() {
        if (modbusMaster != null) {
            modbusMaster.disconnect();
        }
        if (statusTimer != null) {
            statusTimer.cancel();
        }
        if (statusConsumer != null) {
            statusConsumer.accept(false);
        }
    }

    public boolean isConnected() {
        return modbusMaster != null && modbusMaster.isConnected();
    }

    public void setConnectionStatusListener(Consumer<Boolean> statusConsumer) {
        this.statusConsumer = statusConsumer;
    }

    public InsModbusMaster getModbusMaster() {
        return modbusMaster;
    }

    public Connection getConnection() {
        return connection.get();
    }

    public void setConnection(Connection connection) {
        this.connection.set(connection);
    }

    public SimpleObjectProperty<Connection> connectionProperty() {
        return connection;
    }

    private TimerTask getStatusTimerTask() {
        return new TimerTask() {
            private Boolean prevConnected;

            @Override
            public void run() {
                Boolean connected = isConnected();
                if (!connected.equals(prevConnected)) {
                    prevConnected = connected;
                    if (statusConsumer != null) {
                        statusConsumer.accept(connected);
                    }
                }
            }
        };
    }

    private boolean validateConnection(Connection activeConnection, List<String> errors) {
        if (activeConnection == null) {
            errors.add("Connection: Should be specified");
        } else {
            String ipAddress = activeConnection.getIpAddress();
            Integer port = activeConnection.getPort();
            Integer unitId = activeConnection.getUnitId();
            Integer timeout = activeConnection.getTimeout();
            Integer scanTime = activeConnection.getScanTime();

            if (ipAddress == null || ipAddress.isEmpty()) {
                errors.add("Connection: Ip address cannot be empty");
            }
            if (port == null) {
                errors.add("Connection: Port cannot be empty");
            }
            if (port != null && port <= 0) {
                errors.add("Connection: Port must be greater then 0");
            }
            if (unitId == null) {
                errors.add("Connection: Unit id cannot be empty");
            }
            if (unitId != null && unitId < 0) {
                errors.add("Connection: Unit id must be greater then 0");
            }
            if (timeout == null) {
                errors.add("Connection: Timeout cannot be empty");
            }
            if (timeout != null && timeout < 0) {
                errors.add("Connection: Timeout cannot be lesser then 0");
            }
            if (scanTime == null) {
                errors.add("Connection: Scan time cannot be empty");
            }
            if (scanTime != null && scanTime < 0) {
                errors.add("Connection: Scan time cannot be lesser then 0");
            }
        }
        return errors.isEmpty();
    }

}
