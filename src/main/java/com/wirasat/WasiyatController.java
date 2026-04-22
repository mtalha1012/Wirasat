package com.wirasat;

import com.wirasat.model.FamilyMember;
import com.wirasat.model.Wasiyat;
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
 * Shows wasiyat scoped to the principal deceased.
 * Uses the shared AssetCard.fxml for consistent presentation.
 */
public class WasiyatController implements Initializable {

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
        // Only show deceased's wasiyat (schema: wasiyat.deceased_id)
        for (Wasiyat w : db.getDeceasedWasiyats()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/wirasat/AssetCard.fxml"));
                VBox cardNode = loader.load();
                CardController cc = loader.getController();

                String benName = w.getBeneficiaryId() == 0 ? "External Charity" : "Unknown";
                FamilyMember ben = db.getMemberById(w.getBeneficiaryId());
                if (ben != null) benName = ben.getName();

                cc.setCardData("Wasiyat #" + w.getWillId(), "WASIYAT", format.format(w.getAmount()),
                        benName, "WILL", "#f59e0b");
                cc.setOnEdit(() -> showEditDialog(w));
                cc.setOnDelete(() -> { db.removeWasiyat(w); refreshUI(); });

                cardsContainer.getChildren().add(cardNode);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void showEditDialog(Wasiyat w) {
        Dialog<Wasiyat> dialog = new Dialog<>();
        dialog.setTitle("Edit Wasiyat");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);

        ComboBox<String> benCombo = new ComboBox<>();
        benCombo.getItems().add("External Charity (ID 0)");
        for (FamilyMember m : db.getFamilyMembers()) {
            benCombo.getItems().add(m.getMemberId() + " : " + m.getName());
        }
        FamilyMember curBen = db.getMemberById(w.getBeneficiaryId());
        benCombo.setValue(curBen != null ? curBen.getMemberId() + " : " + curBen.getName() : "External Charity (ID 0)");

        TextField amountField = new TextField(w.getAmount() != null ? w.getAmount().toPlainString() : "");

        grid.add(new Label("Beneficiary:"), 0, 0); grid.add(benCombo, 1, 0);
        grid.add(new Label("Amount (Rs):"), 0, 1); grid.add(amountField, 1, 1);
        dialog.getDialogPane().setContent(grid); dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try { w.setAmount(new BigDecimal(amountField.getText())); } catch (Exception ignored) {}
                String benVal = benCombo.getValue();
                if (benVal != null && benVal.contains(" : ")) {
                    try { w.setBeneficiaryId(Integer.parseInt(benVal.split(" : ")[0])); } catch (Exception ignored) {}
                } else { w.setBeneficiaryId(0); }
                return w;
            }
            return null;
        });
        dialog.showAndWait().ifPresent(x -> refreshUI());
    }

    @FXML
    private void handleAddWasiyat(ActionEvent event) {
        FamilyMember deceased = db.getDeceased();
        if (deceased == null) {
            new Alert(Alert.AlertType.WARNING, "Set a principal deceased first.").showAndWait();
            return;
        }

        Dialog<Wasiyat> dialog = new Dialog<>();
        dialog.setTitle("Add Wasiyat");
        dialog.setHeaderText("New will from " + deceased.getName() + " (max 1/3 of estate)");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        ComboBox<String> benCombo = new ComboBox<>();
        benCombo.getItems().add("External Charity (ID 0)");
        for (FamilyMember m : db.getFamilyMembers()) {
            benCombo.getItems().add(m.getMemberId() + " : " + m.getName());
        }
        benCombo.setValue("External Charity (ID 0)");
        TextField amountField = new TextField(); amountField.setPromptText("Amount in Rs");

        grid.add(new Label("Beneficiary:"), 0, 0); grid.add(benCombo, 1, 0);
        grid.add(new Label("Amount (Rs):"), 0, 1); grid.add(amountField, 1, 1);
        dialog.getDialogPane().setContent(grid); dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                BigDecimal amt = BigDecimal.ZERO;
                try { amt = new BigDecimal(amountField.getText()); } catch (Exception ignored) {}
                int benId = 0;
                String benVal = benCombo.getValue();
                if (benVal != null && benVal.contains(" : ")) {
                    try { benId = Integer.parseInt(benVal.split(" : ")[0]); } catch (Exception ignored) {}
                }
                return new Wasiyat(0, deceased.getMemberId(), benId, amt);
            }
            return null;
        });
        dialog.showAndWait().ifPresent(w -> { db.addWasiyat(w); refreshUI(); });
    }
}
