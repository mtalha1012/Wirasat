package com.wirasat;

import com.wirasat.model.DeceasedHeir;
import com.wirasat.model.FamilyMember;
import com.wirasat.model.RelationType;
import com.wirasat.service.MockDataService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Shows all heir-to-deceased mappings for the principal deceased.
 * Allows adding new mappings and deleting existing ones.
 */
public class HeirsRelationsController implements Initializable {

    @FXML private TableView<DeceasedHeir> relationsTable;
    @FXML private TableColumn<DeceasedHeir, String> colDeceased;
    @FXML private TableColumn<DeceasedHeir, String> colHeir;
    @FXML private TableColumn<DeceasedHeir, String> colRelation;
    @FXML private TableColumn<DeceasedHeir, String> colCategory;
    @FXML private TableColumn<DeceasedHeir, Void> colActions;

    private MockDataService db;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        db = MockDataService.getInstance();

        colDeceased.setCellValueFactory(data -> {
            FamilyMember m = db.getMemberById(data.getValue().getDeceasedId());
            return new ReadOnlyObjectWrapper<>(m != null ? m.getName() : "Unknown");
        });
        colHeir.setCellValueFactory(data -> {
            FamilyMember m = db.getMemberById(data.getValue().getHeirId());
            return new ReadOnlyObjectWrapper<>(m != null ? m.getName() : "Unknown");
        });
        colRelation.setCellValueFactory(data -> {
            RelationType r = db.getRelationTypeById(data.getValue().getRelationId());
            return new ReadOnlyObjectWrapper<>(r != null ? r.getRelationName() : "Unknown");
        });
        colCategory.setCellValueFactory(data -> {
            RelationType r = db.getRelationTypeById(data.getValue().getRelationId());
            return new ReadOnlyObjectWrapper<>(r != null ? r.getCategory() : "N/A");
        });

        // Delete button column
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Remove");
            {
                deleteBtn.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    DeceasedHeir dh = getTableView().getItems().get(getIndex());
                    db.removeDeceasedHeir(dh);
                    refreshTable();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        refreshTable();
    }

    private void refreshTable() {
        // Show only principal deceased's heir mappings
        relationsTable.getItems().setAll(db.getDeceasedHeirMappings());
    }

    @FXML
    private void handleAddMapping(ActionEvent event) {
        FamilyMember deceased = db.getDeceased();
        if (deceased == null) {
            new Alert(Alert.AlertType.WARNING, "Set a principal deceased first.").showAndWait();
            return;
        }

        Dialog<DeceasedHeir> dialog = new Dialog<>();
        dialog.setTitle("Map New Heir Relation");
        dialog.setHeaderText("Map an heir to " + deceased.getName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/wirasat/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dark-panel");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        // Heir selection — exclude the deceased themselves
        ComboBox<FamilyMember> heirCombo = new ComboBox<>();
        for (FamilyMember m : db.getFamilyMembers()) {
            if (m.getMemberId() != deceased.getMemberId()) heirCombo.getItems().add(m);
        }
        heirCombo.setConverter(new javafx.util.StringConverter<FamilyMember>() {
            @Override public String toString(FamilyMember m) { return m != null ? m.getName() : ""; }
            @Override public FamilyMember fromString(String s) { return null; }
        });

        // Relation type — all 20 from seed.sql
        ComboBox<RelationType> relationCombo = new ComboBox<>();
        relationCombo.getItems().addAll(db.getRelationTypes());
        relationCombo.setConverter(new javafx.util.StringConverter<RelationType>() {
            @Override public String toString(RelationType r) { return r != null ? r.getRelationName() + " (" + r.getCategory() + ")" : ""; }
            @Override public RelationType fromString(String s) { return null; }
        });

        grid.add(new Label("Deceased:"), 0, 0);
        grid.add(new Label(deceased.getName()), 1, 0);
        grid.add(new Label("Heir:"), 0, 1); grid.add(heirCombo, 1, 1);
        grid.add(new Label("Relation:"), 0, 2); grid.add(relationCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(450);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && heirCombo.getValue() != null && relationCombo.getValue() != null) {
                return new DeceasedHeir(0, deceased.getMemberId(),
                        heirCombo.getValue().getMemberId(), relationCombo.getValue().getRelationId());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(mapping -> {
            db.addDeceasedHeir(mapping);
            refreshTable();
        });
    }
}

