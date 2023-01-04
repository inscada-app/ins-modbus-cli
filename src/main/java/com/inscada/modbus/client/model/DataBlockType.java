package com.inscada.modbus.client.model;

public enum DataBlockType {

    Coils("Coils"),
    DiscreteInputs("Discrete Inputs"),
    HoldingRegisters("Holding Registers"),
    InputRegisters("Input Registers");

    private String val;
    DataBlockType(String val){
        this.val = val;
    }

    @Override
    public String toString() {
        return val;
    }
}
