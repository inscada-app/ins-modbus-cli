package com.inscada.modbus.client.controllers;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.inscada.modbus.client.model.HoldingInputRegisterFunctions;
import com.inscada.modbus.client.services.ConnectionService;
import com.inscada.modbus.client.services.InsModbusMaster;
import com.inscada.modbus.client.services.Statistics;
import com.inscada.modbus.client.utils.Constraints;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

import static com.inscada.modbus.client.model.HoldingInputRegisterFunctions.*;

public class HoldingInputRegistersController implements Initializable {

    private final Stage holdingInputRegisterStage;
    private final ConnectionService connectionService;
    private final ObservableList<AddressIntValuePair> observableList;
    private final IntegerStringConverter integerStringConverter = new IntegerStringConverter();
    private final SimpleObjectProperty<HoldingInputRegisterFunctions> func = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<DataViewType> type = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Integer> startAddress = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Integer> quantity = new SimpleObjectProperty<>();
    private final SimpleStringProperty message = new SimpleStringProperty();
    private final SimpleBooleanProperty isError = new SimpleBooleanProperty();


    @FXML
    TextField txtFieldStartAddress;
    @FXML
    TextField txtFieldQuantity;
    @FXML
    ComboBox<HoldingInputRegisterFunctions> comboBoxFunctions;
    @FXML
    TableView<AddressIntValuePair> tblView;
    @FXML
    TableColumn<AddressIntValuePair, Integer> tblColumnAddress;
    @FXML
    TableColumn<AddressIntValuePair, Integer> tblColumnValue;
    @FXML
    Label lblStatus;
    @FXML
    Button btnExecute;
    @FXML
    ComboBox<DataViewType> comboBoxTypes;

    public HoldingInputRegistersController(ConnectionService connectionService) {
        this.connectionService = connectionService;
        this.observableList = FXCollections.observableArrayList();
        this.holdingInputRegisterStage = new Stage();
        loadStage();
    }

    private void loadStage() {
        FXMLLoader fxmlLoader = new FXMLLoader(SetValueController.class.getResource("/com/inscada/modbus/client/holding-input-function-view.fxml"));
        fxmlLoader.setController(this);
        try {
            holdingInputRegisterStage.setScene(new Scene(fxmlLoader.load(), 700, 400));
            holdingInputRegisterStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/inscada.png")));
            holdingInputRegisterStage.setResizable(false);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        holdingInputRegisterStage.setTitle("Holding / Input Registers");
    }

    public void showStage() {
        holdingInputRegisterStage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initConstraints();
        initBindings();
        initTableView();
        initValues();
        onExecuteButtonClick();
    }

    private void initConstraints() {
        Constraints.setIntTextField(txtFieldStartAddress);
        Constraints.setIntTextField(txtFieldQuantity);
    }

    private void initBindings() {
        func.addListener((observableValue, oldVal, newVal) -> {
            if (newVal == HoldingInputRegisterFunctions.WriteSingleRegister) {
                quantity.setValue(1);
            }
            if (newVal == ReadInputRegisters) {
                startAddress.setValue(30001);
            }
            if (oldVal == ReadInputRegisters) {
                startAddress.setValue(40001);
            }
        });

        startAddress.addListener(observable -> setListItems());
        quantity.addListener(observable -> setListItems());

        comboBoxFunctions.valueProperty().bindBidirectional(func);
        comboBoxTypes.valueProperty().bindBidirectional(type);
        txtFieldStartAddress.textProperty().bindBidirectional(startAddress, integerStringConverter);
        txtFieldQuantity.textProperty().bindBidirectional(quantity, integerStringConverter);
        lblStatus.textProperty().bind(message);
        lblStatus.textFillProperty().bind(Bindings.when(isError).then(Color.RED).otherwise(Color.BLACK));
    }

    private void initTableView() {
        tblView.setEditable(true);
        tblColumnValue.setEditable(true);
        tblColumnAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        tblColumnValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        type.addListener(observable -> tblView.refresh());
        tblColumnValue.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<>() {
            @Override
            public String toString(Integer val) {
                if (val == null) {
                    return null;
                }
                switch (type.getValue()) {
                    case Signed:
                        return val.shortValue() + "";

                    case Hex:
                        return String.format("%04X", val);

                    case Unsigned:
                        return (val & 0xFFFF) + "";
                }
                return val + "";
            }

            @Override
            public Integer fromString(String s) {
                switch (type.getValue()) {
                    case Hex:
                        try {
                            int i = Integer.parseInt(s, 16);
                            if (i < 0 || i > 65535) return null;
                            return i;
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    case Signed:
                        try {
                            int i = Integer.parseInt(s);
                            if (i < Short.MIN_VALUE || i > Short.MAX_VALUE) return null;
                            return i;
                        } catch (NumberFormatException e) {
                            return null;
                        }

                    case Unsigned:
                        try {
                            int i = Integer.parseInt(s);
                            if (i < 0 || i > 65535) return null;
                            return i;
                        } catch (NumberFormatException e) {
                            return null;
                        }

                }
                return null;
            }
        }));
        tblView.setItems(observableList);
    }

    private void initValues() {
        ObservableList<HoldingInputRegisterFunctions> functions = FXCollections.unmodifiableObservableList(
                FXCollections.observableArrayList(HoldingInputRegisterFunctions.values()));
        comboBoxFunctions.setItems(functions);
        startAddress.setValue(40001);
        quantity.setValue(1);
        func.setValue(functions.get(0));

        ObservableList<DataViewType> dataViewTypes = FXCollections.unmodifiableObservableList(
                FXCollections.observableArrayList(DataViewType.values()));
        comboBoxTypes.setItems(dataViewTypes);
        type.setValue(dataViewTypes.get(0));
    }

    private void setListItems() {
        Integer address = startAddress.getValue();
        Integer amount = quantity.getValue();
        if (!validateRange()) return;
        int currentSize = observableList.size();
        if (currentSize < amount) {
            IntStream.range(currentSize, amount).forEachOrdered(i -> observableList.add(new AddressIntValuePair(-1, 0)));
        } else {
            observableList.remove(amount, currentSize);
        }
        for (int i = 0; i < observableList.size(); i++) {
            observableList.get(i).setAddress(address + i);
        }
    }

    private boolean validateRange() {
        String error = null;
        if (startAddress.get() == null || quantity.get() == null) {
            error = "Start address and quantity should be specified";
        }
        if (error == null && func.get() == ReadInputRegisters && (startAddress.get() < 30001 || startAddress.get() > 39999)) {
            error = "Start Address should be in range [30001-39999]";
        }
        if (error == null && func.get() == ReadInputRegisters && (startAddress.get() + quantity.get() > 40000)) {
            error = String.format("Amount should be in range [1-%d]", 40000 - startAddress.get());
        }
        if (error == null && func.get() != ReadInputRegisters && (startAddress.get() < 40001 || startAddress.get() > 49999)) {
            error = "Start Address should be in range [40001-49999]";
        }
        if (error == null && func.get() != ReadInputRegisters && (startAddress.get() + quantity.get() > 50000)) {
            error = String.format("Amount should be in range [1-%d]", 50000 - startAddress.get());
        }
        if (error == null && quantity.get() < 1) {
            error = "Quantity should be positive";
        }
        if (error == null && (func.get() == ReadInputRegisters || func.get() == ReadHoldingRegisters) && quantity.get() > 125) {
            error = "Quantity max value is 125";
        }
        if (error == null && func.get() == WriteSingleRegister && quantity.get() > 1) {
            error = "Quantity max value is 1";
        }
        if (error == null && func.get() == WriteMultipleRegisters && quantity.get() > 123) {
            error = "Quantity max value is 123";
        }
        isError.set(error != null);
        message.set(error);
        return !isError.get();
    }

    private void execute() {
        if (!connectionService.isConnected()) {
            message.set("Not Connected");
            isError.set(true);
            return;
        }

        InsModbusMaster modbusMaster = connectionService.getModbusMaster();
        int unitId = connectionService.getConnection().getUnitId();
        Integer address = startAddress.get();
        Integer amount = quantity.get();

        if (!validateRange()) return;
        try {
            switch (comboBoxFunctions.getValue()) {
                case ReadHoldingRegisters:
                    Register[] holdingRegisters = modbusMaster.readMultipleRegisters(unitId, address - 40001, amount);
                    for (int i = 0; i < observableList.size(); i++) {
                        observableList.get(i).setValue(holdingRegisters[i].getValue());
                    }
                    Statistics.success();
                    message.set("Holding registers succesfully read");
                    break;
                case ReadInputRegisters:
                    InputRegister[] inputRegisters = modbusMaster.readInputRegisters(unitId, address - 30001, amount);
                    for (int i = 0; i < observableList.size(); i++) {
                        observableList.get(i).setValue(inputRegisters[i].getValue());
                    }
                    Statistics.success();
                    message.set("Input Registers succesfully read");
                    break;
                case WriteSingleRegister:
                    AddressIntValuePair pair = observableList.get(0);
                    Register register = new SimpleRegister(pair.getValue());
                    modbusMaster.writeSingleRegister(unitId, pair.getAddress() - 40001, register);
                    Statistics.success();
                    message.set("Single register written");
                    break;
                case WriteMultipleRegisters:
                    Register[] registers = new Register[observableList.size()];
                    for (int i = 0; i < observableList.size(); i++) {
                        registers[i] = new SimpleRegister(observableList.get(i).getValue());
                    }
                    modbusMaster.writeMultipleRegisters(unitId, address - 40001, registers);
                    Statistics.success();
                    message.set("Multiple registers written");
                    break;
            }
            isError.set(false);
        } catch (ModbusException e) {
            Statistics.error();
            message.set(e.getMessage());
            isError.set(true);
        } catch (Exception e) {
            message.set(e.getMessage());
            isError.set(true);
        }
    }

    private void onExecuteButtonClick() {
        btnExecute.setOnMouseClicked(event -> execute());
    }

    public enum DataViewType {
        Signed, Unsigned, Hex;
    }

    public class AddressIntValuePair {
        private final SimpleIntegerProperty address;
        private final SimpleIntegerProperty value;

        public AddressIntValuePair(int address, int value) {
            this.address = new SimpleIntegerProperty(address);
            this.value = new SimpleIntegerProperty(value);
        }

        public int getAddress() {
            return address.get();
        }

        public void setAddress(int address) {
            this.address.set(address);
        }

        public SimpleIntegerProperty addressProperty() {
            return address;
        }

        public int getValue() {
            return value.get();
        }

        public void setValue(int value) {
            this.value.set(value);
        }

        public SimpleIntegerProperty valueProperty() {
            return value;
        }
    }

}
