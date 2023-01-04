package com.inscada.modbus.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;

public class Connection {

    private final ReadOnlyStringProperty ipAddress;
    private final ReadOnlyObjectWrapper<Integer> port;
    private final ReadOnlyObjectWrapper<Integer> unitId;
    private final ReadOnlyObjectWrapper<Integer> timeout;
    private final ReadOnlyObjectWrapper<Integer> scanTime;
    private final ReadOnlyObjectProperty<ConnectionType> connectionType;

    @JsonCreator
    public Connection(@JsonProperty("ipAddress") String ipAddress, @JsonProperty("port") Integer port,
                      @JsonProperty("unitId") Integer unitId,@JsonProperty("timeout") Integer timeout,
                      @JsonProperty("scanTime") Integer scanTime,@JsonProperty("connectionType") ConnectionType connectionType) {
        this.ipAddress = new ReadOnlyStringWrapper(ipAddress);
        this.port = new ReadOnlyObjectWrapper<>(port);
        this.unitId = new ReadOnlyObjectWrapper<>(unitId);
        this.timeout = new ReadOnlyObjectWrapper<>(timeout);
        this.scanTime = new ReadOnlyObjectWrapper<>(scanTime);
        this.connectionType = new ReadOnlyObjectWrapper<>(connectionType);
    }

    public Connection() {
        this("127.0.0.1", 502, 1, 3000, 1000, ConnectionType.ModbusTcp);
    }

    public String getIpAddress() {
        return ipAddress.get();
    }

    public ReadOnlyStringProperty ipAddressProperty() {
        return ipAddress;
    }

    public Integer getPort() {
        return port.get();
    }

    public ReadOnlyObjectWrapper<Integer> portProperty() {
        return port;
    }

    public void setPort(Integer port) {
        this.port.set(port);
    }

    public Integer getUnitId() {
        return unitId.get();
    }

    public ReadOnlyObjectWrapper<Integer> unitIdProperty() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId.set(unitId);
    }

    public Integer getTimeout() {
        return timeout.get();
    }

    public ReadOnlyObjectWrapper<Integer> timeoutProperty() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout.set(timeout);
    }

    public Integer getScanTime() {
        return scanTime.get();
    }

    public ReadOnlyObjectWrapper<Integer> scanTimeProperty() {
        return scanTime;
    }

    public void setScanTime(Integer scanTime) {
        this.scanTime.set(scanTime);
    }

    public ConnectionType getConnectionType() {
        return connectionType.get();
    }

    public ReadOnlyObjectProperty<ConnectionType> connectionTypeProperty() {
        return connectionType;
    }
}
