package com.inscada.modbus.client.model;

import java.time.LocalDateTime;

public class TagValue {
    private final Object val;
    private final LocalDateTime date;

    public TagValue(Object val, LocalDateTime date) {
        this.val = val;
        this.date = date;
    }

    public Object getVal() {
        return val;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
