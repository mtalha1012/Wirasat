package com.wirasat;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class CardController {
    @FXML private Pane topColorBand;
    @FXML private Label titleLabel;
    @FXML private Label categoryLabel;
    @FXML private Label valueLabel;
    @FXML private Label ownerLabel;
    @FXML private Label statusLabel;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    public void setCardData(String title, String category, String value, String owner, String statusText, String colorCode) {
        titleLabel.setText(title);
        categoryLabel.setText(category);
        valueLabel.setText(value);
        ownerLabel.setText(owner);
        statusLabel.setText(statusText);
        topColorBand.setStyle("-fx-background-color: " + colorCode + ";");
    }
    
    public void setOnEdit(Runnable handler) {
        editBtn.setOnAction(e -> handler.run());
    }
    
    public void setOnDelete(Runnable handler) {
        deleteBtn.setOnAction(e -> handler.run());
    }
    
    public void hideActions() {
        editBtn.setVisible(false);
        editBtn.setManaged(false);
        deleteBtn.setVisible(false);
        deleteBtn.setManaged(false);
    }
}