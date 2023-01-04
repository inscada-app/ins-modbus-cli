package com.inscada.modbus.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;

import java.time.LocalDateTime;

public class Tag {
    private final ReadOnlyStringProperty dataBlockName;
    private final ReadOnlyStringProperty name;
    private final ReadOnlyObjectWrapper<Integer> address;
    private final ReadOnlyObjectProperty<TagType> type;
    private final ReadOnlyBooleanProperty byteSwap;
    private final ReadOnlyBooleanProperty wordSwap;
    @JsonIgnore
    private final SimpleObjectProperty<TagValue> value;

    @JsonCreator
    public Tag(@JsonProperty("dataBlockName") String dataBlockName, @JsonProperty("name") String name,
               @JsonProperty("address") Integer address, @JsonProperty("type") TagType type,
               @JsonProperty("byteSwap") boolean byteSwap, @JsonProperty("wordSwap") boolean wordSwap) {
        this.dataBlockName = new ReadOnlyStringWrapper(dataBlockName);
        this.name = new ReadOnlyStringWrapper(name);
        this.address = new ReadOnlyObjectWrapper<>(address);
        this.type = new ReadOnlyObjectWrapper<>(type);
        this.byteSwap = new ReadOnlyBooleanWrapper(byteSwap);
        this.wordSwap = new ReadOnlyBooleanWrapper(wordSwap);
        this.value = new SimpleObjectProperty<>(new TagValue(null, LocalDateTime.now()));
    }

    public String getDataBlockName() {
        return dataBlockName.get();
    }

    public ReadOnlyStringProperty dataBlockNameProperty() {
        return dataBlockName;
    }

    public String getName() {
        return name.get();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name;
    }

    public Integer getAddress() {
        return address.get();
    }

    public ReadOnlyObjectWrapper<Integer> addressProperty() {
        return address;
    }

    public TagType getType() {
        return type.get();
    }

    public ReadOnlyObjectProperty<TagType> typeProperty() {
        return type;
    }

    public boolean isByteSwap() {
        return byteSwap.get();
    }

    public ReadOnlyBooleanProperty byteSwapProperty() {
        return byteSwap;
    }

    public boolean isWordSwap() {
        return wordSwap.get();
    }

    public ReadOnlyBooleanProperty wordSwapProperty() {
        return wordSwap;
    }

    public TagValue getValue() {
        return value.get();
    }

    public void setValue(TagValue value) {
        this.value.set(value);
    }

    public SimpleObjectProperty<TagValue> valueProperty() {
        return value;
    }
}
