package com.inscada.modbus.client.controllers;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.inscada.modbus.client.services.ConnectionService;
import com.inscada.modbus.client.services.InsModbusMaster;
import com.inscada.modbus.client.services.Statistics;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class MaskWriteRegisterController implements Initializable {

    private final Stage maskWriteRegisterStage;
    private final ConnectionService connectionService;
    private final IntegerStringConverter integerStringConverter = new IntegerStringConverter();
    private final SimpleObjectProperty<Integer> address = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Integer> andMask = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Integer> orMask = new SimpleObjectProperty<>();
    private final SimpleStringProperty message = new SimpleStringProperty();
    private final SimpleBooleanProperty isError = new SimpleBooleanProperty();

    private final StringConverter<Integer> binaryIntStrConverter = new StringConverter<>() {
        @Override
        public String toString(Integer i) {
            String s = String.format("%16s", Integer.toBinaryString(i)).replace(' ', '0');
            return s.replaceAll("(.{4})", "$1 ");
        }

        @Override
        public Integer fromString(String s) {
            return s.isBlank() ? null : Integer.parseInt(s.replaceAll("\\s", ""), 2);
        }
    };

    @FXML
    TextField txtFieldAddress;
    @FXML
    TextField txtFieldAndMask;
    @FXML
    TextField txtFieldOrMask;
    @FXML
    Button btnExecute;
    @FXML
    Label lblStatus;

    public MaskWriteRegisterController(ConnectionService connectionService) {
        this.maskWriteRegisterStage = new Stage();
        this.connectionService = connectionService;
        loadStage();
    }

    private void loadStage() {
        FXMLLoader fxmlLoader = new FXMLLoader(SetValueController.class.getResource("/com/inscada/modbus/client/mask-write-register-view.fxml"));
        fxmlLoader.setController(this);
        try {
            maskWriteRegisterStage.setScene(new Scene(fxmlLoader.load(), 500, 250));
            maskWriteRegisterStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/inscada.png")));
            maskWriteRegisterStage.setResizable(false);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        maskWriteRegisterStage.setTitle("Mask Write Register");
    }

    public void showStage() {
        maskWriteRegisterStage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initValues();
        initBindings();
        onExecuteButtonClick();
    }

    private void initValues() {
        address.set(40001);
        andMask.set(0);
        orMask.set(0);
    }

    private void initBindings() {
        txtFieldAddress.textProperty().bindBidirectional(address, integerStringConverter);

        Pattern binaryPattern = Pattern.compile("([0,1]{4}\\s){3}[0,1]{4}");
        TextFormatter<Integer> andMaskTextFormatter = binaryTextFormatter(binaryPattern);
        txtFieldAndMask.setTextFormatter(andMaskTextFormatter);
        andMaskTextFormatter.valueProperty().bindBidirectional(andMask);

        TextFormatter<Integer> orMaskTextFormatter = binaryTextFormatter(binaryPattern);
        txtFieldOrMask.setTextFormatter(orMaskTextFormatter);
        orMaskTextFormatter.valueProperty().bindBidirectional(orMask);

        lblStatus.textProperty().bind(message);
        lblStatus.textFillProperty().bind(Bindings.when(isError).then(Color.RED).otherwise(Color.BLACK));
    }

    private TextFormatter<Integer> binaryTextFormatter(Pattern pattern) {
        return new TextFormatter<>(binaryIntStrConverter, 0, change -> {
            if (pattern.matcher(change.getControlNewText()).matches()) {
                return change; // allow this change to happen
            } else {
                return null; // prevent change
            }
        });
    }

    private boolean validationRange() {
        String error = null;
        if (address.get() > 49999 || address.get() < 40001) {
            error = "Address should be in range [40001-49999]";
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
        if (!validationRange()) {
            return;
        }
        try {
            InsModbusMaster modbusMaster = connectionService.getModbusMaster();
            int unitId = connectionService.getConnection().getUnitId();
            int addr = address.get();
            int andMaskVal = andMask.get();
            int orMaskVal = orMask.get();
            modbusMaster.maskWriteRegister(unitId, addr - 40001, andMaskVal, orMaskVal);
            Statistics.success();
            isError.set(false);
            message.set("Succesfully written");
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
}
