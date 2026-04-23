package com.wirasat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/wirasat/MainLayout.fxml"));
            Scene scene = new Scene(root, 1100, 700);
            primaryStage.setTitle("Wirasat - Inheritance Manager");
            com.wirasat.util.GUIUtil.setAppIcon(primaryStage);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
