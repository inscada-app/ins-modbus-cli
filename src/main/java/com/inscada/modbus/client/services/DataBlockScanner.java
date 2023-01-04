package com.inscada.modbus.client.services;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.inscada.modbus.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DataBlockScanner {

    private static final Logger log = LoggerFactory.getLogger("Data Block Scanner");
    private static final int DELAY = 0;
    private final DataBlockService dataBlockService;
    private final InsModbusMaster modbusMaster;
    private final Connection connection;
    private Timer scanTimer;

    public DataBlockScanner(DataBlockService dataBlockService, InsModbusMaster modbusMaster, Connection connection) {
        this.dataBlockService = dataBlockService;
        this.modbusMaster = modbusMaster;
        this.connection = connection;
    }

    public void start() {
        stop();
        scanTimer = new Timer("Scan Timer", true);
        scanTimer.schedule(getScanTimerTask(), DELAY, connection.getScanTime());
    }

    public void stop() {
        if (scanTimer != null) {
            scanTimer.cancel();
        }
    }

    //Scan methods
    private TimerTask getScanTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                List<DataBlock> dataBlocks = dataBlockService.getDataBlocks();
                for (DataBlock dataBlock : dataBlocks) {
                    switch (dataBlock.getType()) {
                        case Coils:
                            scanCoils(dataBlock);
                            break;
                        case DiscreteInputs:
                            scanDiscreteInputs(dataBlock);
                            break;
                        case HoldingRegisters:
                            scanHoldingRegisters(dataBlock);
                            break;
                        case InputRegisters:
                            scanInputRegisters(dataBlock);
                            break;
                    }
                }
            }
        };
    }

    private void scanCoils(DataBlock dataBlock) {
        int address = dataBlock.getStartAddress() - 1;
        try {
            BitVector bitVector = modbusMaster.readCoils(connection.getUnitId(), address, dataBlock.getAmount());
            Statistics.success();
            processBitVector(dataBlock, bitVector);
        } catch (ModbusException e) {
            Statistics.error();
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void scanDiscreteInputs(DataBlock dataBlock) {
        int address = dataBlock.getStartAddress() - 10001;
        try {
            BitVector bitVector = modbusMaster.readInputDiscretes(connection.getUnitId(), address, dataBlock.getAmount());
            Statistics.success();
            processBitVector(dataBlock, bitVector);
        } catch (ModbusException e) {
            Statistics.error();
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void scanHoldingRegisters(DataBlock dataBlock) {
        int address = dataBlock.getStartAddress() - 40001;
        try {
            Register[] registers = modbusMaster.readMultipleRegisters(connection.getUnitId(), address, dataBlock.getAmount());
            Statistics.success();
            processRegisters(dataBlock, registers);
        } catch (ModbusException e) {
            Statistics.error();
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void scanInputRegisters(DataBlock dataBlock) {
        int address = dataBlock.getStartAddress() - 30001;
        try {
            InputRegister[] registers = modbusMaster.readInputRegisters(connection.getUnitId(), address, dataBlock.getAmount());
            processRegisters(dataBlock, registers);
            Statistics.success();
        } catch (ModbusException e) {
            Statistics.error();
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void processBitVector(DataBlock dataBlock, BitVector bitVector) {
        LocalDateTime date = LocalDateTime.now();
        List<Tag> tags = dataBlockService.getTags(dataBlock.getName());
        for (Tag tag : tags) {
            try {
                int address = tag.getAddress();
                Boolean b = bitVector.getBit(address - dataBlock.getStartAddress());
                tag.setValue(new TagValue(b, date));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    private void processRegisters(DataBlock dataBlock, InputRegister[] registers) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(registers.length * 2);
        Arrays.stream(registers).map(InputRegister::toBytes).forEachOrdered(byteBuffer::put);
        LocalDateTime date = LocalDateTime.now();
        List<Tag> tags = dataBlockService.getTags(dataBlock.getName());
        for (Tag tag : tags) {
            try {
                int address = tag.getAddress() - dataBlock.getStartAddress();
                switch (tag.getType()) {
                    case Boolean:
                        processBooleanTag(byteBuffer, date, tag, address);
                        break;
                    case Byte:
                        processByteTag(byteBuffer, date, tag, address);
                        break;
                    case Short:
                        processShortTag(byteBuffer, date, tag, address);
                        break;
                    case Integer:
                        processIntegerTag(byteBuffer, date, tag, address);
                        break;
                    case Long:
                        processLongTag(byteBuffer, date, tag, address);
                        break;
                    case Float:
                        processFloatTag(byteBuffer, date, tag, address);
                        break;
                    case Double:
                        processDoubleTag(byteBuffer, date, tag, address);
                        break;
                    case UnsignedShort:
                        processUnsignedShort(byteBuffer, date, tag, address);
                        break;
                    case UnsignedInteger:
                        processUnsignedInt(byteBuffer, date, tag, address);
                        break;
                    case UnsignedByte:
                        processUnsignedByte(byteBuffer, date, tag, address);
                        break;
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    private byte[] getByteArrayFromBuffer(ByteBuffer byteBuffer, Tag tag, int byteAddress) {
        byte[] bytes = new byte[getByteArrayLength(tag.getType())];
        byteBuffer.position(2 * byteAddress);
        byteBuffer.get(bytes, 0, bytes.length);
        swap(tag, bytes);
        return bytes;
    }

    private void swap(Tag tag, byte[] bytes) {
        if (tag.isByteSwap()) {
            for (int i = 1; i < bytes.length; i += 2) {
                byte temp = bytes[i - 1];
                bytes[i - 1] = bytes[i];
                bytes[i] = temp;
            }
        }
        if (tag.isWordSwap()) {
            for (int i = 2; i < bytes.length; i += 4) {
                byte temp1 = bytes[i - 1];
                byte temp2 = bytes[i - 2];
                bytes[i - 1] = bytes[i + 1];
                bytes[i - 2] = bytes[i];
                bytes[i] = temp2;
                bytes[i + 1] = temp1;
            }
        }
    }

    private int getByteArrayLength(TagType tagType) {
        switch (tagType) {
            case Boolean:
            case Byte:
            case Short:
            case UnsignedShort:
            case UnsignedByte:
                return 2;
            case Integer:
            case Float:
            case UnsignedInteger:
                return 4;
            case Long:
            case Double:
                return 8;
            default:
                throw new IllegalArgumentException("Invalid tag type: " + tagType);
        }
    }

    private void processDoubleTag(ByteBuffer byteBuffer, LocalDateTime date, Tag tag, int byteAddress) {
        byte[] bytes = getByteArrayFromBuffer(byteBuffer, tag, byteAddress);
        Double d = ByteBuffer.wrap(bytes).getDouble();
        tag.setValue(new TagValue(d, date));
    }

    private void processFloatTag(ByteBuffer byteBuffer, LocalDateTime date, Tag tag, int byteAddress) {
        byte[] bytes = getByteArrayFromBuffer(byteBuffer, tag, byteAddress);
        Float f = ByteBuffer.wrap(bytes).getFloat();
        tag.setValue(new TagValue(f, date));
    }

    private void processLongTag(ByteBuffer byteBuffer, LocalDateTime date, Tag tag, int byteAddress) {
        byte[] bytes = getByteArrayFromBuffer(byteBuffer, tag, byteAddress);
        Long l = ByteBuffer.wrap(bytes).getLong();
        tag.setValue(new TagValue(l, date));
    }

    private void processIntegerTag(ByteBuffer byteBuffer, LocalDateTime date, Tag tag, int byteAddress) {
        byte[] bytes = getByteArrayFromBuffer(byteBuffer, tag, byteAddress);
        Integer i = ByteBuffer.wrap(bytes).getInt();
        tag.setValue(new TagValue(i, date));
    }

    private void processShortTag(ByteBuffer byteBuffer, LocalDateTime date, Tag tag, int byteAddress) {
        byte[] bytes = getByteArrayFromBuffer(byteBuffer, tag, byteAddress);
        Short s = ByteBuffer.wrap(bytes).getShort();
        tag.setValue(new TagValue(s, date));
    }

    private void processByteTag(ByteBuffer byteBuffer, LocalDateTime date, Tag tag, int byteAddress) {
        byte[] bytes = getByteArrayFromBuffer(byteBuffer, tag, byteAddress);
        Byte b = bytes[0];
        tag.setValue(new TagValue(b, date));
    }

    private void processBooleanTag(ByteBuffer byteBuffer, LocalDateTime date, Tag tag, int byteAddress) {
        byte[] bytes = getByteArrayFromBuffer(byteBuffer, tag, byteAddress);
        Boolean b = bytes[0] != 0;
        tag.setValue(new TagValue(b, date));
    }

    private void processUnsignedShort(ByteBuffer byteBuffer, LocalDateTime date, Tag tag, int byteAddress) {
        byte[] bytes = getByteArrayFromBuffer(byteBuffer, tag, byteAddress);
        int i = toUnsignedShort(bytes);
        tag.setValue(new TagValue(i, date));
    }

    private Integer toUnsignedShort(byte[] b) {
        // precedence & (and) is higher than | (or)
        int i = 0;
        i |= b[0] & 0xFF;
        i <<= 8;
        i |= b[1] & 0xFF;
        return i;
    }

    private void processUnsignedInt(ByteBuffer byteBuffer, LocalDateTime date, Tag tag, int byteAddress) {
        byte[] bytes = getByteArrayFromBuffer(byteBuffer, tag, byteAddress);
        Long i = toUnsignedInt(bytes);
        tag.setValue(new TagValue(i, date));
    }

    private Long toUnsignedInt(byte[] b) {
        // precedence & (and) is higher than | (or)
        long i = 0;
        i |= b[0] & 0xFF;
        i <<= 8;
        i |= b[1] & 0xFF;
        i <<= 8;
        i |= b[2] & 0xFF;
        i <<= 8;
        i |= b[3] & 0xFF;
        return i;
    }

    private void processUnsignedByte(ByteBuffer byteBuffer, LocalDateTime date, Tag tag, int byteAddress) {
        byte[] bytes = getByteArrayFromBuffer(byteBuffer, tag, byteAddress);
        int i = Byte.toUnsignedInt(bytes[0]);
        tag.setValue(new TagValue(i, date));
    }

    //Set value methods

    public void setValue(Tag tag, Object newValue) {
        DataBlock dataBlock = dataBlockService.getDataBlock(tag.getDataBlockName()).orElse(null);
        if (dataBlock == null) {
            log.error("No DataBlock found");
            return;
        }
        if (dataBlock.getType() == DataBlockType.Coils) {
            writeCoil(tag, newValue);
        }
        if (dataBlock.getType() == DataBlockType.HoldingRegisters) {
            writeHoldingRegisters(tag, newValue);
        }
    }

    private void writeCoil(Tag tag, Object newValue) {
        try {
            boolean state = Boolean.parseBoolean(newValue.toString());
            int address = tag.getAddress() - 1;
            modbusMaster.writeCoil(connection.getUnitId(), address, state);
            Statistics.success();
        } catch (ModbusException e) {
            Statistics.error();
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void writeHoldingRegisters(Tag tag, Object newValue) {
        try {
            int address = tag.getAddress() - 40001;
            byte[] bytes = getByteArrayFromValue(tag, newValue);
            if (bytes.length > 2) {
                writeMultipleRegisters(address, bytes);
            } else {
                writeSingleRegister(address, bytes);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void writeSingleRegister(int address, byte[] bytes) {
        try {
            Register register = new SimpleRegister(bytes[0], bytes[1]);
            modbusMaster.writeSingleRegister(connection.getUnitId(), address, register);
            Statistics.success();
        } catch (ModbusException e) {
            Statistics.error();
        }
    }

    private void writeMultipleRegisters(int address, byte[] bytes) {
        try {
            Register[] registers = new SimpleRegister[bytes.length / 2];
            for (int i = 0; i < registers.length; i++) {
                registers[i] = new SimpleRegister(bytes[i * 2], bytes[i * 2 + 1]);
            }
            modbusMaster.writeMultipleRegisters(connection.getUnitId(), address, registers);
            Statistics.success();
        } catch (ModbusException e) {
            Statistics.error();
        }
    }

    private byte[] getByteArrayFromValue(Tag tag, Object newValue) {
        byte[] bytes = new byte[getByteArrayLength(tag.getType())];
        switch (tag.getType()) {
            case Boolean:
                boolean b = Boolean.parseBoolean(newValue.toString());
                bytes[0] = (byte) (!b ? 0 : 1);
                bytes[1] = (byte) 0;
                break;
            case Byte:
                byte byteVal = Byte.parseByte(newValue.toString());
                bytes[0] = byteVal;
                bytes[1] = (byte) 0;
                break;
            case Short:
                short s = Short.parseShort(newValue.toString());
                ByteBuffer byteBufferShort = ByteBuffer.wrap(bytes);
                byteBufferShort.putShort(s);
                break;
            case Integer:
                int i = Integer.parseInt(newValue.toString());
                ByteBuffer byteBufferInt = ByteBuffer.wrap(bytes);
                byteBufferInt.putInt(i);
                break;
            case Long:
                long l = Long.parseLong(newValue.toString());
                ByteBuffer byteBufferLong = ByteBuffer.wrap(bytes);
                byteBufferLong.putLong(l);
                break;
            case Float:
                float f = Float.parseFloat(newValue.toString());
                ByteBuffer byteBufferFloat = ByteBuffer.wrap(bytes);
                byteBufferFloat.putFloat(f);
                break;
            case Double:
                double d = Double.parseDouble(newValue.toString());
                ByteBuffer byteBufferDouble = ByteBuffer.wrap(bytes);
                byteBufferDouble.putDouble(d);
                break;
            case UnsignedInteger:
                long unsignedInt = Long.parseLong(newValue.toString());
                unsignedIntegerToBytes(unsignedInt, bytes);
                break;
            case UnsignedShort:
                int unsignedShort = Integer.parseInt(newValue.toString());
                unsignedShortToBytes(unsignedShort, bytes);
                break;
            case UnsignedByte:
                int unsignedByte = Integer.parseInt(newValue.toString());
                bytes[0] = (byte) unsignedByte;
                bytes[1] = (byte) 0;
                break;
        }
        swap(tag, bytes);
        return bytes;
    }

    private void unsignedIntegerToBytes(long v, byte[] bytes) {
        bytes[0] = (byte) (0xff & (v >> 24));
        bytes[1] = (byte) (0xff & (v >> 16));
        bytes[2] = (byte) (0xff & (v >> 8));
        bytes[3] = (byte) (0xff & v);
    }

    private void unsignedShortToBytes(int v, byte[] bytes) {
        bytes[0] = (byte) (0xff & (v >> 8));
        bytes[1] = (byte) (0xff & v);
    }
}
