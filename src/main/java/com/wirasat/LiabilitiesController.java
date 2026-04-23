package com.wirasat;

import com.wirasat.model.FamilyMember;
import com.wirasat.model.Liability;
import com.wirasat.service.MockDataService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Shows liabilities scoped to the principal deceased.
 * Uses the shared AssetCard.fxml for consistent presentation.
 */
public class LiabilitiesController implements Initializable {

    @FXML private FlowPane cardsContainer;
    private MockDataService db;
    private NumberFormat format;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        db = MockDataService.getInstance();
        format = NumberFormat.getCurrencyInstance(new Locale("en", "PK"));
        refreshUI();
    }

    private void refreshUI() {
        cardsContainer.getChildren().clear();
        // Only show deceased's liabilities (schema: liabilities.deceased_id)
        for (Liability l : db.getDeceasedLiabilities()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/wirasat/AssetCard.fxml"));
                VBox cardNode = loader.load();
                CardController cc = loader.getController();

                FamilyMember owner = db.getMemberById(l.getDeceasedId());
                cc.setCardData(l.getDetails(), "LIABILITY", format.format(l.getAmount()),
                        owner != null ? owner.getName() : "Unknown", "DEBT", "#ef4444");
                cc.setOnEdit(() -> showEditDialog(l));
                cc.setOnDelete(() -> { db.removeLiability(l); refreshUI(); });

                cardsContainer.getChildren().add(cardNode);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void showEditDialog(Liability l) {
        Dialog<Liability> dialog = new Dialog<>();
        dialog.setTitle("Edit Liability");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/wirasat/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dark-panel");
        javafx.stage.Window window = dialog.getDialogPane().getScene().getWindow();
        if (window instanceof javafx.stage.Stage) { com.wirasat.util.GUIUtil.setAppIcon((javafx.stage.Stage) window); }
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField detailsField = new TextField(l.getDetails());
        TextField amountField = new TextField(l.getAmount() != null ? l.getAmount().toPlainString() : "");

        grid.add(new Label("Details:"), 0, 0); grid.add(detailsField, 1, 0);
        grid.add(new Label("Amount (Rs):"), 0, 1); grid.add(amountField, 1, 1);
        dialog.getDialogPane().setContent(grid); dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                l.setDetails(detailsField.getText());
                try { l.setAmount(new BigDecimal(amountField.getText())); } catch (Exception ignored) {}
                return l;
            }
            return null;
        });
        dialog.showAndWait().ifPresent(x -> refreshUI());
    }

    @FXML
    private void handleAddLiability(ActionEvent event) {
        FamilyMember deceased = db.getDeceased();
        if (deceased == null) {
            com.wirasat.util.GUIUtil.showAlert(javafx.scene.control.Alert.AlertType.WARNING, null, "Set a principal deceased first.");
            return;
        }

        Dialog<Liability> dialog = new Dialog<>();
        dialog.setTitle("Add Liability");
        dialog.setHeaderText("New debt owed by " + deceased.getName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/wirasat/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dark-panel");
        javafx.stage.Window window = dialog.getDialogPane().getScene().getWindow();
        if (window instanceof javafx.stage.Stage) { com.wirasat.util.GUIUtil.setAppIcon((javafx.stage.Stage) window); }
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField detailsField = new TextField(); detailsField.setPromptText("e.g. Bank Loan — HBL");
        TextField amountField = new TextField(); amountField.setPromptText("Amount in Rs");

        grid.add(new Label("Details:"), 0, 0); grid.add(detailsField, 1, 0);
        grid.add(new Label("Amount (Rs):"), 0, 1); grid.add(amountField, 1, 1);
        dialog.getDialogPane().setContent(grid); dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                BigDecimal amt = BigDecimal.ZERO;
                try { amt = new BigDecimal(amountField.getText()); } catch (Exception ignored) {}
                // Always assign to principal deceased (schema: liabilities.deceased_id)
                return new Liability(0, detailsField.getText(), amt, deceased.getMemberId());
            }
            return null;
        });
        dialog.showAndWait().ifPresent(l -> { db.addLiability(l); refreshUI(); });
    }
}
