package com.inscada.modbus.client.controllers;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements Initializable {
    private final Stage aboutStage;
    private final HostServices hostServices;
    @FXML
    Hyperlink hyperLinkIcon;
    @FXML
    Hyperlink hyperLinkJ2Mod;
    @FXML
    Hyperlink hyperLinkInScada;
    @FXML
    ImageView imageView;

    public AboutController(HostServices hostServices) {
        this.hostServices = hostServices;
        this.aboutStage = new Stage();
        loadStage();
    }

    private void loadStage() {
        FXMLLoader fxmlLoader = new FXMLLoader(SetValueController.class.getResource("/com/inscada/modbus/client/about-view.fxml"));
        fxmlLoader.setController(this);
        try {
            aboutStage.setScene(new Scene(fxmlLoader.load(), 375, 175));
            aboutStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/inscada.png")));
            aboutStage.initModality(Modality.APPLICATION_MODAL);
            aboutStage.setResizable(false);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        aboutStage.setTitle("About");
    }

    public void showStage() {
        aboutStage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        stageImage();
        onHyperLinkIconClick();
        onHyperLinkModbusClick();
        onHyperLinkInScadaClick();
    }

    private void stageImage(){
        imageView.setImage(new Image(getClass().getResourceAsStream("/icons/inscada.png")));
        imageView.setFitWidth(50);
        imageView.setFitHeight(50);
    }

    private void onHyperLinkIconClick() {
        hyperLinkIcon.setOnMouseClicked(event -> hostServices.showDocument(hyperLinkIcon.getText()));
    }

    private void onHyperLinkModbusClick() {
        hyperLinkJ2Mod.setOnMouseClicked(event -> hostServices.showDocument(hyperLinkJ2Mod.getText()));
    }

    private void onHyperLinkInScadaClick(){
        hyperLinkInScada.setOnMouseClicked(event -> hostServices.showDocument(hyperLinkInScada.getText()));
    }

}
