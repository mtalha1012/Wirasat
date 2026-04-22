package com.wirasat;

import com.wirasat.model.Asset;
import com.wirasat.model.AssetType;
import com.wirasat.model.FamilyMember;
import com.wirasat.service.AssetService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;

public class AddAssetDialogController {

    @FXML private TextField nameField;
    @FXML private ComboBox<AssetType> categoryCombo;
    @FXML private TextField valueField;
    @FXML private ComboBox<FamilyMember> ownerCombo;
    @FXML private CheckBox shareableCheck;
    @FXML private Label errorLabel;

    private AssetService assetService;
    private Asset createdAsset = null;

    public void setAssetService(AssetService service) {
        this.assetService = service;
        populateCombos();
    }

    private void populateCombos() {
        // Setup Category Combo
        categoryCombo.getItems().addAll(assetService.getAssetTypes());
        categoryCombo.setConverter(new StringConverter<AssetType>() {
            @Override
            public String toString(AssetType type) {
                return type != null ? type.getCategoryName() : "";
            }
            @Override
            public AssetType fromString(String s) {
                return null;
            }
        });

        // Setup Owner Combo
        ownerCombo.getItems().addAll(assetService.getPossibleOwners());
        ownerCombo.setConverter(new StringConverter<FamilyMember>() {
            @Override
            public String toString(FamilyMember member) {
                return member != null ? member.getName() : "";
            }
            @Override
            public FamilyMember fromString(String s) {
                return null;
            }
        });
    }

    public Asset getCreatedAsset() {
        return createdAsset;
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText();
        AssetType type = categoryCombo.getValue();
        FamilyMember owner = ownerCombo.getValue();
        String valueStr = valueField.getText();
        
        if (name == null || name.trim().isEmpty() || type == null || owner == null || valueStr == null || valueStr.trim().isEmpty()) {
            errorLabel.setText("Please fill out all fields.");
            errorLabel.setVisible(true);
            return;
        }

        BigDecimal value;
        try {
            value = new BigDecimal(valueStr);
        } catch(NumberFormatException e) {
            errorLabel.setText("Invalid format for value.");
            errorLabel.setVisible(true);
            return;
        }

        createdAsset = new Asset();
        createdAsset.setAssetName(name);
        createdAsset.setTypeId(type.getTypeId());
        createdAsset.setOwnerId(owner.getMemberId());
        createdAsset.setValue(value);
        createdAsset.setShareable(shareableCheck.isSelected());

        closeDialog(event);
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        createdAsset = null;
        closeDialog(event);
    }

    private void closeDialog(ActionEvent event) {
        javafx.scene.Node source = (javafx.scene.Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
