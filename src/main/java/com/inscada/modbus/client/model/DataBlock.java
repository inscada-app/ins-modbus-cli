package com.inscada.modbus.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;

public class DataBlock {
    private final ReadOnlyStringProperty name;
    private final ReadOnlyObjectProperty<DataBlockType> type;
    private final ReadOnlyObjectProperty<Integer> startAddress;
    private final ReadOnlyObjectProperty<Integer> amount;

    @JsonCreator
    public DataBlock(@JsonProperty("name") String name, @JsonProperty("type") DataBlockType type,
                     @JsonProperty("startAddress") Integer startAddress, @JsonProperty("amount") Integer amount) {
        this.name =  new ReadOnlyStringWrapper(name);
        this.type =  new ReadOnlyObjectWrapper<>(type);
        this.startAddress =  new ReadOnlyObjectWrapper<>(startAddress);
        this.amount = new ReadOnlyObjectWrapper<>(amount);
    }

    public String getName() {
        return name.get();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name;
    }

    public DataBlockType getType() {
        return type.get();
    }

    public ReadOnlyObjectProperty<DataBlockType> typeProperty() {
        return type;
    }

    public Integer getStartAddress() {
        return startAddress.get();
    }

    public ReadOnlyObjectProperty<Integer> startAddressProperty() {
        return startAddress;
    }

    public Integer getAmount() {
        return amount.get();
    }

    public ReadOnlyObjectProperty<Integer> amountProperty() {
        return amount;
    }
}
