package com.wirasat;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private StackPane contentArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load default view
        loadView("/com/wirasat/DashboardView.fxml");
    }

    @FXML private void showDashboard(ActionEvent event) { updateActiveNav(event); loadView("/com/wirasat/DashboardView.fxml"); }
    @FXML private void showFamilyMembers(ActionEvent event) { updateActiveNav(event); loadView("/com/wirasat/FamilyMembersView.fxml"); }
    @FXML private void showAssets(ActionEvent event) { updateActiveNav(event); loadView("/com/wirasat/AssetsView.fxml"); }
    @FXML private void showLiabilities(ActionEvent event) { updateActiveNav(event); loadView("/com/wirasat/LiabilitiesView.fxml"); }
    @FXML private void showWasiyat(ActionEvent event) { updateActiveNav(event); loadView("/com/wirasat/WasiyatView.fxml"); }
    @FXML private void showDistribution(ActionEvent event) { updateActiveNav(event); loadView("/com/wirasat/DistributionView.fxml"); }
    @FXML private void showHeirsRelations(ActionEvent event) { updateActiveNav(event); loadView("/com/wirasat/HeirsRelationsView.fxml"); }
    
    @FXML private void showPlaceholder(ActionEvent event) { updateActiveNav(event); loadView("/com/wirasat/PlaceholderView.fxml"); }
    
    private void updateActiveNav(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        
        if (clickedBtn.getParent() != null) {
            for (Node node : clickedBtn.getParent().getChildrenUnmodifiable()) {
                if (node instanceof Button) {
                    node.getStyleClass().remove("active-nav");
                }
            }
        }
        
        if (!clickedBtn.getStyleClass().contains("active-nav")) {
            clickedBtn.getStyleClass().add("active-nav");
        }
    }

    private void loadView(String fxmlFile) {
        System.out.println("Loading route: " + fxmlFile);
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlFile));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
