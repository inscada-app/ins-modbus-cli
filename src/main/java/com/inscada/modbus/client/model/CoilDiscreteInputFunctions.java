package com.inscada.modbus.client.model;

public enum CoilDiscreteInputFunctions {

    ReadCoils("Read Coils (0x01)"),
    ReadDiscreteInputs("Read Discrete Inputs (0x02)"),
    WriteSingleCoil("Write Single Coil (0x05)"),
    WriteMultipleCoils("Write Multiple Coils (0x0F)");

    private final String val;

    CoilDiscreteInputFunctions(String val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return val;
    }
}
