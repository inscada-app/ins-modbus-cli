package com.inscada.modbus.client.model;

public enum HoldingInputRegisterFunctions {
    ReadHoldingRegisters("Read Holding Registers (0x03)"),
    ReadInputRegisters("Read Input Registers (0x04)"),
    WriteSingleRegister("Write Single Register (0x06)"),
    WriteMultipleRegisters("Write Multiple Registers (0x10)");

    private final String val;

    HoldingInputRegisterFunctions(String val){
        this.val = val;
    }

    @Override
    public String toString() {
        return val;
    }
}
