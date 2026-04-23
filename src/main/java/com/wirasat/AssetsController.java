package com.wirasat;

import com.wirasat.model.Asset;
import com.wirasat.model.AssetType;
import com.wirasat.model.FamilyMember;
import com.wirasat.service.AssetService;
import com.wirasat.service.MockAssetService;
import com.wirasat.service.MockDataService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class AssetsController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<AssetType> categoryCombo;

    private AssetService assetService;
    private MockDataService db;
    private NumberFormat format;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assetService = new MockAssetService();
        db = MockDataService.getInstance();
        format = NumberFormat.getCurrencyInstance(new Locale("en", "PK"));
        setupCategoryCombo();
        setupSearchListeners();
        loadAssetsToUI(assetService.getAllAssets());
    }
    
    private void setupCategoryCombo() {
        categoryCombo.getItems().clear();
        AssetType allType = new AssetType(0, "All Categories");
        categoryCombo.getItems().add(allType);
        categoryCombo.getItems().addAll(assetService.getAssetTypes());
        categoryCombo.setConverter(new StringConverter<AssetType>() {
            @Override public String toString(AssetType o) { return o != null ? o.getCategoryName() : ""; }
            @Override public AssetType fromString(String s) { return null; }
        });
        categoryCombo.setValue(allType);
        categoryCombo.setOnAction(e -> filterAssets());
    }
    
    private void setupSearchListeners() {
        searchField.textProperty().addListener((o, ov, nv) -> filterAssets());
    }
    
    private void filterAssets() {
        String keyword = searchField.getText();
        AssetType selectedType = categoryCombo.getValue();
        Integer categoryId = (selectedType != null && selectedType.getTypeId() > 0) ? selectedType.getTypeId() : null;
        loadAssetsToUI(assetService.searchAssets(keyword, categoryId));
    }

    public void loadAssetsToUI(List<Asset> assets) {
        cardsContainer.getChildren().clear();
        for (Asset asset : assets) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/wirasat/AssetCard.fxml"));
                VBox cardNode = loader.load();
                CardController cc = loader.getController();
                
                AssetType type = assetService.getAssetTypeById(asset.getTypeId());
                FamilyMember owner = assetService.getOwnerById(asset.getOwnerId());
                String catName = type != null ? type.getCategoryName() : "Unknown";
                String ownerName = owner != null ? owner.getName() : "Unknown";
                String valueStr = asset.getValue() != null ? format.format(asset.getValue()) : "Rs 0";
                
                cc.setCardData(asset.getAssetName(), catName, valueStr, ownerName,
                    asset.isShareable() ? "SHAREABLE" : "NON-SHAREABLE",
                    getColorForCategory(asset.getTypeId()));
                
                cc.setOnEdit(() -> showEditDialog(asset));
                cc.setOnDelete(() -> {
                    db.removeAsset(asset);
                    filterAssets();
                });

                cardsContainer.getChildren().add(cardNode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void showEditDialog(Asset asset) {
        Dialog<Asset> dialog = new Dialog<>();
        dialog.setTitle("Edit Asset");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/wirasat/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dark-panel");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField nameField = new TextField(asset.getAssetName());
        TextField valueField = new TextField(asset.getValue() != null ? asset.getValue().toPlainString() : "");
        
        ComboBox<AssetType> catBox = new ComboBox<>();
        catBox.getItems().addAll(assetService.getAssetTypes());
        catBox.setConverter(new StringConverter<AssetType>() {
            @Override public String toString(AssetType t) { return t != null ? t.getCategoryName() : ""; }
            @Override public AssetType fromString(String s) { return null; }
        });
        AssetType cur = assetService.getAssetTypeById(asset.getTypeId());
        if (cur != null) catBox.setValue(cur);
        
        ComboBox<FamilyMember> ownerBox = new ComboBox<>();
        ownerBox.getItems().addAll(assetService.getPossibleOwners());
        ownerBox.setConverter(new StringConverter<FamilyMember>() {
            @Override public String toString(FamilyMember m) { return m != null ? m.getName() : ""; }
            @Override public FamilyMember fromString(String s) { return null; }
        });
        FamilyMember curOwner = assetService.getOwnerById(asset.getOwnerId());
        if (curOwner != null) ownerBox.setValue(curOwner);
        
        CheckBox shareCheck = new CheckBox();
        shareCheck.setSelected(asset.isShareable());

        grid.add(new Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1); grid.add(catBox, 1, 1);
        grid.add(new Label("Value (Rs):"), 0, 2); grid.add(valueField, 1, 2);
        grid.add(new Label("Owner:"), 0, 3); grid.add(ownerBox, 1, 3);
        grid.add(new Label("Shareable:"), 0, 4); grid.add(shareCheck, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                asset.setAssetName(nameField.getText());
                try { asset.setValue(new BigDecimal(valueField.getText())); } catch (Exception ignored) {}
                if (catBox.getValue() != null) asset.setTypeId(catBox.getValue().getTypeId());
                if (ownerBox.getValue() != null) asset.setOwnerId(ownerBox.getValue().getMemberId());
                asset.setShareable(shareCheck.isSelected());
                return asset;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(a -> filterAssets());
    }
    
    private String getColorForCategory(int categoryId) {
        switch (categoryId) {
            case 1: return "#E2B93B";
            case 2: return "#38bdf8";
            case 3: return "#f59e0b";
            case 4: return "#0ea5e9";
            case 5: return "#a78bfa";
            default: return "#9ca3af";
        }
    }
    
    @FXML
    private void handleAddAsset(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/wirasat/AddAssetDialog.fxml"));
            javafx.scene.Parent root = loader.load();
            AddAssetDialogController dialogController = loader.getController();
            dialogController.setAssetService(assetService);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Add New Asset");
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(((javafx.scene.Node) event.getSource()).getScene().getWindow());
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/wirasat/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            Asset newAsset = dialogController.getCreatedAsset();
            if (newAsset != null) {
                assetService.addAsset(newAsset);
                filterAssets();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}