package com.inscada.modbus.client.model;

public enum TagType {
    Boolean("Boolean"),
    Byte("Byte"),
    Short("Short"),
    Integer("Integer"),
    Long("Long"),
    Float("Float"),
    Double("Double"),
    UnsignedByte("Unsigned Byte"),
    UnsignedShort("Unsigned Short"),
    UnsignedInteger("Unsigned Integer");

    private final String val;
    TagType(String val) {
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
