package com.inscada.modbus.client.controllers;

import com.inscada.modbus.client.model.Tag;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class SetValueController implements Initializable {
    private final Stage setValueStage;
    @FXML
    Label lblTagName;
    @FXML
    Label lblOldValue;
    @FXML
    TextField txtFieldNewValue;
    @FXML
    Button btnSetValue;
    @FXML
    Button btnCancel;

    public SetValueController() {
        this.setValueStage = new Stage();
        loadStage();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        btnCancel.setOnMouseClicked(event -> {
            txtFieldNewValue.clear();
            setValueStage.close();
        });
        btnSetValue.setOnMouseClicked(event -> setValueStage.close());
    }

    private void loadStage() {
        FXMLLoader fxmlLoader = new FXMLLoader(SetValueController.class.getResource("/com/inscada/modbus/client/set-value-view.fxml"));
        fxmlLoader.setController(this);
        try {
            setValueStage.setScene(new Scene(fxmlLoader.load(), 370, 170));
            setValueStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/inscada.png")));
            setValueStage.initModality(Modality.APPLICATION_MODAL);
            setValueStage.setResizable(false);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setValueStage.setTitle("Set Value");
    }

    public void showModal(Tag tag) {
        lblTagName.setText(tag.getName());
        String oldValue = tag.getValue().getVal() == null ? null : tag.getValue().getVal().toString();
        lblOldValue.setText(oldValue);
        setValueStage.showAndWait();
    }

    public String getNewValue() {
        return txtFieldNewValue.getText();
    }
}
