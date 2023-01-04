package com.inscada.modbus.client.services;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadWriteMultipleRequest;
import com.ghgande.j2mod.modbus.msg.ReadWriteMultipleResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

public class InsModbusTCPMaster extends ModbusTCPMaster implements InsModbusMaster {

    private ReadWriteMultipleRequest readWriteMultipleRequest;

    public InsModbusTCPMaster(String addr, int port, int timeout, boolean reconnect, boolean useRtuOverTcp) {
        super(addr, port, timeout, reconnect, useRtuOverTcp);
    }

    public synchronized InputRegister[] readWriteMultipleRegisters(int unit, int readRef, int readCount, int writeRef, Register[] registers) throws ModbusException {
        checkTransaction();
        if (readWriteMultipleRequest == null) {
            readWriteMultipleRequest = new ReadWriteMultipleRequest();
        }
        readWriteMultipleRequest.setUnitID(unit);
        readWriteMultipleRequest.setReadReference(readRef);
        readWriteMultipleRequest.setReadWordCount(readCount);
        readWriteMultipleRequest.setWriteReference(writeRef);
        readWriteMultipleRequest.setRegisters(registers);
        transaction.setRequest(readWriteMultipleRequest);
        transaction.execute();

        ReadWriteMultipleResponse response = (ReadWriteMultipleResponse) getAndCheckResponse();
        return response.getRegisters();
    }

    @Override
    public synchronized void connect() throws Exception {
        super.connect();
        if (transaction != null) {
            transaction.setRetries(1);
        }
    }

    protected void checkTransaction() throws ModbusException {
        if (transaction == null) {
            throw new ModbusException("No transaction created, probably not connected");
        }
    }

    protected ModbusResponse getAndCheckResponse() throws ModbusException {
        ModbusResponse res = transaction.getResponse();
        if (res == null) {
            throw new ModbusException("No response");
        }
        return res;
    }
}
