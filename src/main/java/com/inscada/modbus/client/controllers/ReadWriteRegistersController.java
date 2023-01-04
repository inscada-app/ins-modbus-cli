package com.inscada.modbus.client.controllers;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.inscada.modbus.client.services.ConnectionService;
import com.inscada.modbus.client.services.InsModbusMaster;
import com.inscada.modbus.client.services.InsModbusTCPMaster;
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

public class ReadWriteRegistersController implements Initializable {

    private final Stage readWriteRegisterStage;
    private final ConnectionService connectionService;
    private final IntegerStringConverter integerStringConverter = new IntegerStringConverter();
    private final SimpleObjectProperty<Integer> writeAddress = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Integer> writeQuantity = new SimpleObjectProperty<>();
    private final SimpleStringProperty message = new SimpleStringProperty();
    private final SimpleBooleanProperty isError = new SimpleBooleanProperty();
    private final ObservableList<ReadWriteRegistersController.AddressIntegerValuePair> observableListWrite;
    private final ObservableList<ReadWriteRegistersController.AddressIntegerValuePair> observableListRead;
    private final SimpleObjectProperty<DataViewType> writeType = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<DataViewType> readType = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Integer> readAddress = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Integer> readQuantity = new SimpleObjectProperty<>();
    @FXML
    TextField txtFieldWriteAddress;
    @FXML
    TextField txtFieldWriteQuantity;
    @FXML
    ComboBox<DataViewType> comboBoxWriteType;
    @FXML
    ComboBox<DataViewType> comboBoxReadType;
    @FXML
    TextField txtFieldReadAddress;
    @FXML
    TextField txtFieldReadQuantity;
    @FXML
    TableView<AddressIntegerValuePair> tblViewWrite;
    @FXML
    TableColumn<AddressIntegerValuePair, Integer> tblColumnWriteAddress;
    @FXML
    TableColumn<AddressIntegerValuePair, Integer> tblColumnWriteValue;
    @FXML
    TableView<AddressIntegerValuePair> tblViewRead;
    @FXML
    TableColumn<AddressIntegerValuePair, Integer> tblColumnReadAddress;
    @FXML
    TableColumn<AddressIntegerValuePair, Integer> tblColumnReadValue;
    @FXML
    Button btnExecute;
    @FXML
    Label lblStatus;

    public ReadWriteRegistersController(ConnectionService connectionService) {
        this.connectionService = connectionService;
        this.observableListWrite = FXCollections.observableArrayList();
        this.observableListRead = FXCollections.observableArrayList();
        this.readWriteRegisterStage = new Stage();
        loadStage();
    }

    private void loadStage() {
        FXMLLoader fxmlLoader = new FXMLLoader(SetValueController.class.getResource("/com/inscada/modbus/client/read-write-registers-view.fxml"));
        fxmlLoader.setController(this);
        try {
            readWriteRegisterStage.setScene(new Scene(fxmlLoader.load(), 700, 400));
            readWriteRegisterStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/inscada.png")));
            readWriteRegisterStage.setResizable(false);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        readWriteRegisterStage.setTitle("Read / Write Registers");
    }

    public void showStage() {
        readWriteRegisterStage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initConstraints();
        initBindings();
        initWriteTable();
        initValues();
        onExecuteButtonClick();
    }

    private void initValues() {
        ObservableList<DataViewType> types = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(DataViewType.values()));
        comboBoxWriteType.setItems(types);
        writeType.setValue(types.get(0));

        comboBoxReadType.setItems(types);
        readType.setValue(types.get(0));

        writeAddress.set(40001);
        writeQuantity.set(1);
    }

    private void initWriteTable() {
        tblViewWrite.setEditable(true);
        tblColumnWriteValue.setEditable(true);
        tblColumnWriteAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        tblColumnWriteValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        writeType.addListener(observable -> tblViewWrite.refresh());
        tblColumnWriteValue.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<>() {
            @Override
            public String toString(Integer val) {
                if (val == null) {
                    return null;
                }
                switch (writeType.getValue()) {
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
                switch (writeType.getValue()) {
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
        tblViewWrite.setItems(observableListWrite);
    }

    private void initReadTable() {
        tblColumnReadAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        tblColumnReadValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        tblColumnReadValue.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<>() {
            @Override
            public String toString(Integer val) {
                if (val == null) {
                    return null;
                }
                switch (readType.getValue()) {
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
                switch (readType.getValue()) {
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
        tblViewRead.setItems(observableListRead);

    }

    private void initConstraints() {
        Constraints.setIntTextField(txtFieldWriteAddress);
        Constraints.setIntTextField(txtFieldWriteQuantity);
        Constraints.setIntTextField(txtFieldReadAddress);
        Constraints.setIntTextField(txtFieldReadQuantity);
    }

    private void initBindings() {
        writeAddress.addListener(observable -> setWriteList());
        writeQuantity.addListener(observable -> setWriteList());

        txtFieldReadAddress.textProperty().bindBidirectional(readAddress, integerStringConverter);
        txtFieldReadQuantity.textProperty().bindBidirectional(readQuantity, integerStringConverter);
        txtFieldWriteAddress.textProperty().bindBidirectional(writeAddress, integerStringConverter);
        txtFieldWriteQuantity.textProperty().bindBidirectional(writeQuantity, integerStringConverter);

        comboBoxWriteType.valueProperty().bindBidirectional(writeType);
        comboBoxReadType.valueProperty().bindBidirectional(readType);

        lblStatus.textProperty().bind(message);
        lblStatus.textFillProperty().bind(Bindings.when(isError).then(Color.RED).otherwise(Color.BLACK));
    }

    private void setWriteList() {
        Integer address = writeAddress.getValue();
        Integer amount = writeQuantity.getValue();
        if (!validateWriteRange()) return;
        int currentSize = observableListWrite.size();
        if (currentSize < amount) {
            IntStream.range(currentSize, amount).forEachOrdered(i -> observableListWrite.add(new ReadWriteRegistersController.AddressIntegerValuePair(-1, 0)));
        } else {
            observableListWrite.remove(amount, currentSize);
        }
        for (int i = 0; i < observableListWrite.size(); i++) {
            observableListWrite.get(i).setAddress(address + i);
        }
    }

    private boolean validateReadRange() {
        String error = null;
        if (readAddress.get() == null || readQuantity.get() == null) {
            error = "Read Address and Read Quantity should be specified";
        }
        if (error == null && (readAddress.get() < 40001 || readAddress.get() > 49999)) {
            error = "Read Address should be in range [40001-49999]";
        }
        if (error == null && (readAddress.get() + readQuantity.get() > 50000)) {
            error = String.format("Read address should be in range [1-%d]", 50000 - writeAddress.get());
        }
        if (error == null && readQuantity.get() < 1) {
            error = "Read Quantity should be positive";
        }
        if (error == null && readQuantity.get() > 125) {
            error = "Read Quantity max value is 125";
        }
        isError.set(error != null);
        message.set(error);
        return !isError.get();
    }

    private boolean validateWriteRange() {
        String error = null;
        if (writeAddress.get() == null || writeQuantity.get() == null) {
            error = "Write Address and Write Quantity should be specified";
        }
        if (error == null && (writeAddress.get() < 40001 || writeAddress.get() > 49999)) {
            error = "Write Address should be in range [40001-49999]";
        }
        if (error == null && (writeAddress.get() + writeQuantity.get() > 50000)) {
            error = String.format("Write address should be in range [1-%d]", 50000 - writeAddress.get());
        }
        if (error == null && writeQuantity.get() < 1) {
            error = "Write Quantity should be positive";
        }
        if (error == null && writeQuantity.get() > 121) {
            error = "Write Quantity max value is 121";
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
        if (!validateWriteRange() && !validateReadRange()) {
            return;
        }
        InsModbusMaster modbusMaster = connectionService.getModbusMaster();
        int unitID = connectionService.getConnection().getUnitId();
        int writeAddr = writeAddress.getValue() - 40001;
        int writeAmount = writeQuantity.get();
        int readAddr = readAddress.get() - 40001;
        int readAmount = readQuantity.get();
        try {
            Register[] registers = new Register[observableListWrite.size()];
            for (int i = 0; i < observableListWrite.size(); i++) {
                registers[i] = new SimpleRegister(observableListWrite.get(i).getValue());
            }
            InputRegister[] inputRegister = modbusMaster.readWriteMultipleRegisters(unitID, readAddr, readAmount, writeAddr, registers);
            Statistics.success();

            if (observableListRead.size() > 0) {
                observableListRead.clear();
            }
            for (int i = 0; i < readAmount; i++) {
                observableListRead.add(i, new AddressIntegerValuePair(readAddress.getValue() + i, inputRegister[i].getValue()));
            }
            initReadTable();
            message.set("Read / Write succesfull");
        }catch (ModbusException e){
            Statistics.error();
            message.set(e.getMessage());
            isError.set(true);
        }
        catch (Exception e) {
            message.set(e.getMessage());
            isError.set(true);
        }

    }

    private void onExecuteButtonClick() {
        btnExecute.setOnMouseClicked(event -> execute());
    }

    public enum DataViewType {
        Signed, Unsigned, Hex
    }

    public class AddressIntegerValuePair {
        private final SimpleIntegerProperty address;
        private final SimpleIntegerProperty value;

        public AddressIntegerValuePair(int address, int value) {
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
