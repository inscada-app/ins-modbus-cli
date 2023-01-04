package com.inscada.modbus.client.model;

public enum ConnectionType {
    ModbusTcp("Modbus TCP"),
    ModbusRtuOverTcp("Modbus RTU Over TCP"),
    ModbusUDP("Modbus UDP");

    private final String val;

    ConnectionType(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }

    @Override
    public String toString() {
        return val;
    }
}

