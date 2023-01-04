package com.inscada.modbus.client;

import com.inscada.modbus.client.controllers.MainController;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 720);
        stage.setTitle("inSCADA Modbus Client");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/inscada.png")));
        stage.setScene(scene);
        MainController controller = fxmlLoader.getController();
        HostServices hostServices = getHostServices();
        controller.getHostServices(hostServices);
        stage.setOnHidden(e -> {
            controller.shutdown();
            Platform.exit();
        });
        controller.setStage(stage);
        stage.show();

    }
}