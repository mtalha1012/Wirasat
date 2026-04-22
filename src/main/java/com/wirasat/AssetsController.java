package com.wirasat;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AssetsController implements Initializable {

    @FXML
    private FlowPane cardsContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<AssetModel> dummyData = new ArrayList<>();
        dummyData.add(new AssetModel("Main House — Gulberg, Lahore", "Real Estate (Property)", "Rs 2.5Cr", "Muhammad Aslam Khan", true, "#E2B93B"));
        dummyData.add(new AssetModel("DHA Phase 5 Plot", "Real Estate (Property)", "Rs 1.9Cr", "Muhammad Aslam Khan", true, "#E2B93B"));
        dummyData.add(new AssetModel("Allied Bank Savings Account", "Cash at Bank", "Rs 52.0L", "Muhammad Aslam Khan", true, "#38bdf8"));
        dummyData.add(new AssetModel("National Savings Certificate", "Cash at Bank", "Rs 30.0L", "Muhammad Aslam Khan", true, "#10b981"));
        dummyData.add(new AssetModel("Gold Jewellery (42 tola)", "Gold / Silver", "Rs 51.0L", "Muhammad Aslam Khan", true, "#f59e0b"));
        dummyData.add(new AssetModel("Khan Textiles — Business Share", "Business Equity", "Rs 28.0L", "Muhammad Aslam Khan", true, "#0ea5e9"));
        
        loadAssetsToUI(dummyData);
    }

    public void loadAssetsToUI(List<AssetModel> assets) {
        cardsContainer.getChildren().clear(); 

        for (AssetModel asset : assets) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/wirasat/AssetCard.fxml"));
                VBox cardNode = loader.load();
                
                CardController cardCtrl = loader.getController();
                cardCtrl.setCardData(
                    asset.getTitle(), 
                    asset.getCategory(), 
                    asset.getValue(), 
                    asset.getOwner(), 
                    asset.isShareable(),
                    asset.getColorCode() 
                );

                cardsContainer.getChildren().add(cardNode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}