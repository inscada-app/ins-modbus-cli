package com.inscada.modbus.client.services;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.util.BitVector;

public interface InsModbusMaster {

    void connect() throws Exception;

    void disconnect();

    boolean isConnected();

    BitVector readCoils(int unitId, int ref, int count) throws ModbusException;

    boolean writeCoil(int unitId, int ref, boolean state) throws ModbusException;

    void writeMultipleCoils(int unitId, int ref, BitVector coils) throws ModbusException;

    BitVector readInputDiscretes(int unitId, int ref, int count) throws ModbusException;

    InputRegister[] readInputRegisters(int unitId, int ref, int count) throws ModbusException;

    Register[] readMultipleRegisters(int unitId, int ref, int count) throws ModbusException;

    int writeSingleRegister(int unitId, int ref, Register register) throws ModbusException;

    int writeMultipleRegisters(int unitId, int ref, Register[] registers) throws ModbusException;

    boolean maskWriteRegister(int unitId, int ref, int andMask, int orMask) throws ModbusException;

    InputRegister[] readWriteMultipleRegisters(int unit, int readRef, int readCount, int writeRef, Register[] registers) throws ModbusException;
}
