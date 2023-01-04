package com.inscada.modbus.client.controllers;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.inscada.modbus.client.model.CoilDiscreteInputFunctions;
import com.inscada.modbus.client.services.ConnectionService;
import com.inscada.modbus.client.services.InsModbusMaster;
import com.inscada.modbus.client.services.Statistics;
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
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

public class CoilDiscreteInputController implements Initializable {

    private final Stage coilDiscreteInputStage;
    private final ObservableList<AddressBoolValuePair> list;
    private final IntegerStringConverter integerStringConverter = new IntegerStringConverter();

    private final SimpleObjectProperty<CoilDiscreteInputFunctions> func = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Integer> startAddress = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Integer> quantity = new SimpleObjectProperty<>();
    private final SimpleStringProperty message = new SimpleStringProperty();
    private final SimpleBooleanProperty isError = new SimpleBooleanProperty();
    private final ConnectionService connectionService;
    @FXML
    ComboBox<CoilDiscreteInputFunctions> comboBoxFunctions;
    @FXML
    TextField txtFieldStartAddress;
    @FXML
    TextField txtFieldQuantity;
    @FXML
    TableView<AddressBoolValuePair> tblView;
    @FXML
    TableColumn<AddressBoolValuePair, Boolean> tblColumnValue;
    @FXML
    TableColumn<AddressBoolValuePair, Integer> tblColumnAddress;
    @FXML
    Button btnExecute;
    @FXML
    Label lblStatus;


    public CoilDiscreteInputController(ConnectionService connectionService) {
        this.connectionService = connectionService;
        this.list = FXCollections.observableArrayList();
        this.coilDiscreteInputStage = new Stage();
        loadStage();
    }

    private void loadStage() {
        FXMLLoader fxmlLoader = new FXMLLoader(SetValueController.class.getResource("/com/inscada/modbus/client/coil-discrete-function-view.fxml"));
        fxmlLoader.setController(this);
        try {
            coilDiscreteInputStage.setScene(new Scene(fxmlLoader.load(), 700, 400));
            coilDiscreteInputStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/inscada.png")));
            coilDiscreteInputStage.setResizable(false);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        coilDiscreteInputStage.setTitle("Coils / Discrete Inputs");
    }

    public void showStage() {
        coilDiscreteInputStage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initBindings();
        initTableView();
        initValues();
        onExecuteButtonClick();
    }

    private void initTableView() {
        tblColumnAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        tblColumnValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        tblColumnValue.setCellFactory(tableCell -> new CheckBoxTableCell<>());
        tblView.setEditable(true);
        tblView.setItems(list);
    }

    private void initValues() {
        ObservableList<CoilDiscreteInputFunctions> functions = FXCollections.unmodifiableObservableList(
                FXCollections.observableArrayList(CoilDiscreteInputFunctions.values()));
        comboBoxFunctions.setItems(functions);
        startAddress.setValue(1);
        quantity.setValue(1);
        func.setValue(functions.get(0));
    }

    private void initBindings() {
        func.addListener((observableValue, oldVal, newVal) -> {
            if (newVal == CoilDiscreteInputFunctions.WriteSingleCoil) {
                quantity.setValue(1);
            }
            if (newVal == CoilDiscreteInputFunctions.ReadDiscreteInputs) {
                startAddress.setValue(10001);
            }
            if (oldVal == CoilDiscreteInputFunctions.ReadDiscreteInputs) {
                startAddress.setValue(1);
            }
        });

        startAddress.addListener(observable -> setListItems());
        quantity.addListener(observable -> setListItems());

        comboBoxFunctions.valueProperty().bindBidirectional(func);
        txtFieldStartAddress.textProperty().bindBidirectional(startAddress, integerStringConverter);
        txtFieldQuantity.textProperty().bindBidirectional(quantity, integerStringConverter);

        lblStatus.textProperty().bind(message);
        lblStatus.textFillProperty().bind(Bindings.when(isError).then(Color.RED).otherwise(Color.BLACK));
    }

    private void setListItems() {
        Integer address = startAddress.getValue();
        Integer amount = quantity.getValue();
        if (!validateRange()) return;
        int currentSize = list.size();
        if (currentSize < amount) {
            IntStream.range(currentSize, amount).forEachOrdered(i -> list.add(new AddressBoolValuePair(-1, false)));
        } else {
            list.remove(amount, currentSize);
        }
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setAddress(address + i);
        }
    }

    private boolean validateRange() {
        String error = null;
        if (startAddress.get() == null || quantity.get() == null) {
            error = "Start address and quantity should be specified";
        }
        if (error == null && func.get() == CoilDiscreteInputFunctions.ReadDiscreteInputs && (startAddress.get() < 10001 || startAddress.get() > 19999)) {
            error = "Start Address should be in range [10001-19999]";
        }
        if (error == null && func.get() == CoilDiscreteInputFunctions.ReadDiscreteInputs && (startAddress.get() + quantity.get() > 20000)) {
            error = String.format("Quantity should be in range [1-%d]", 20000 - startAddress.get());
        }
        if (error == null && func.get() != CoilDiscreteInputFunctions.ReadDiscreteInputs && (startAddress.get() < 1 || startAddress.get() > 9999)) {
            error = "Start Address should be in range [1-9999]";
        }
        if (error == null && func.get() != CoilDiscreteInputFunctions.ReadDiscreteInputs && (startAddress.get() + quantity.get() > 10000)) {
            error = String.format("Quantity should be in range [1-%d]", 10000 - startAddress.get());
        }
        if (error == null && quantity.get() < 1) {
            error = "Quantity should be positive";
        }
        if (error == null && (func.get() == CoilDiscreteInputFunctions.ReadDiscreteInputs || func.get() == CoilDiscreteInputFunctions.ReadCoils) && quantity.get() > 2000) {
            error = "Quantity max value is 2000";
        }
        if (error == null && func.get() == CoilDiscreteInputFunctions.WriteSingleCoil && quantity.get() > 1) {
            error = "Quantity max value is 1";
        }
        if (error == null && func.get() == CoilDiscreteInputFunctions.WriteMultipleCoils && quantity.get() > 1968) {
            error = "Quantity max value is 1968";
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
        Integer addr = startAddress.getValue();
        Integer amount = quantity.getValue();
        if (!validateRange()) return;
        try {
            switch (comboBoxFunctions.getValue()) {
                case ReadCoils:
                    int coilAddress = addr - 1;
                    BitVector bitVectorCoil = modbusMaster.readCoils(unitId, coilAddress, amount);
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setValue(bitVectorCoil.getBit(i));
                    }
                    Statistics.success();
                    message.set("Coils successfully read");
                    break;
                case ReadDiscreteInputs:
                    int discreteAddress = addr - 10001;
                    BitVector bitVector = modbusMaster.readInputDiscretes(unitId, discreteAddress, amount);
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setValue(bitVector.getBit(i));
                    }
                    Statistics.success();
                    message.set("Discrete Inputs successfully read");
                    break;
                case WriteSingleCoil:
                    modbusMaster.writeCoil(unitId, list.get(0).getAddress(), list.get(0).getValue());
                    Statistics.success();
                    message.set("Single Coil  written");
                    break;
                case WriteMultipleCoils:
                    int writeCoilAddress = addr - 1;
                    BitVector writeBitVector = new BitVector(list.size());
                    for (int i = 0; i < list.size(); i++) {
                        writeBitVector.setBit(i, list.get(i).getValue());
                    }
                    modbusMaster.writeMultipleCoils(unitId, writeCoilAddress, writeBitVector);
                    Statistics.success();
                    message.set("Multiple Coils written");
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

    public class AddressBoolValuePair {

        private final SimpleIntegerProperty address;
        private final SimpleBooleanProperty value;

        public AddressBoolValuePair(int address, boolean value) {
            this.address = new SimpleIntegerProperty(address);
            this.value = new SimpleBooleanProperty(value);
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

        public boolean getValue() {
            return value.get();
        }

        public void setValue(boolean value) {
            this.value.set(value);
        }

        public SimpleBooleanProperty valueProperty() {
            return value;
        }
    }
}
