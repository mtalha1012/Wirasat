package com.wirasat;

import com.wirasat.model.DeceasedHeir;
import com.wirasat.model.FamilyMember;
import com.wirasat.model.RelationType;
import com.wirasat.service.MockDataService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Family tree controller. Tree is ALWAYS rooted at the principal deceased.
 * All date fields use DatePicker to match schema.sql DATETIME requirements.
 * Toggle death uses a date picker so the comparison in
 * FaraidCalculationService.determineLivingHeirs() works correctly:
 *   - heir DOD before deceased DOD → excluded (pre-deceased)
 *   - heir DOD after deceased DOD → included (was alive at time of death)
 *   - heir DOD null → included (alive)
 */
public class FamilyMembersController implements Initializable {

    @FXML private TreeView<String> familyTree;
    @FXML private VBox actionPanel;
    @FXML private Label instructionLabel;
    @FXML private Label selectedNodeLabel;
    @FXML private Label statusLabel;
    @FXML private Label relationLabel;
    @FXML private Button toggleDeathBtn;

    private MockDataService db;
    private FamilyMember selectedMember;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        db = MockDataService.getInstance();
        familyTree.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv != null && nv.getValue() != null) {
                selectedMember = findMemberByLabel(nv.getValue());
                if (selectedMember != null) {
                    actionPanel.setVisible(true);
                    instructionLabel.setVisible(false);
                    updateActionPanel();
                } else {
                    actionPanel.setVisible(false);
                    instructionLabel.setVisible(true);
                }
            }
        });
        refreshTree();
    }

    private FamilyMember findMemberByLabel(String label) {
        for (FamilyMember m : db.getFamilyMembers()) {
            if (label.contains(m.getName())) return m;
        }
        return null;
    }

    private String buildLabel(FamilyMember m) {
        String g = m.getGender() == 'M' ? "♂" : "♀";
        String dob = m.getDateOfBirth() != null ? dateFormat.format(m.getDateOfBirth()) : "?";
        String death = "";
        if (m.getDateOfDeath() != null) {
            death = "  ✝ " + dateFormat.format(m.getDateOfDeath());
        }
        return m.getName() + "  " + g + "  (DOB: " + dob + ")" + death;
    }

    // ===========================================================================
    // TREE
    // ===========================================================================
    private void refreshTree() {
        FamilyMember deceased = db.getDeceased();
        if (deceased == null) {
            TreeItem<String> root = new TreeItem<>("⚠ No Principal Deceased — set one below");
            root.setExpanded(true);
            for (FamilyMember m : db.getFamilyMembers()) {
                root.getChildren().add(new TreeItem<>(buildLabel(m)));
            }
            familyTree.setRoot(root);
            return;
        }

        TreeItem<String> root = new TreeItem<>(buildLabel(deceased) + "  ★ PRINCIPAL");
        root.setExpanded(true);

        // Category branches
        Map<String, TreeItem<String>> catNodes = new LinkedHashMap<>();
        catNodes.put("Spouse", new TreeItem<>("Spouses"));
        catNodes.put("Primary Descendant", new TreeItem<>("Children"));
        catNodes.put("Primary Ascendant", new TreeItem<>("Parents"));
        catNodes.put("Secondary Descendant", new TreeItem<>("Grandchildren"));
        catNodes.put("Secondary Ascendant", new TreeItem<>("Grandparents"));
        catNodes.put("Sibling", new TreeItem<>("Siblings"));
        catNodes.put("Extended", new TreeItem<>("Extended Relatives"));
        catNodes.values().forEach(n -> n.setExpanded(true));

        Set<Integer> mappedIds = new HashSet<>();
        mappedIds.add(deceased.getMemberId());

        for (DeceasedHeir dh : db.getDeceasedHeirMappings()) {
            FamilyMember heir = db.getMemberById(dh.getHeirId());
            RelationType rel = db.getRelationTypeById(dh.getRelationId());
            if (heir == null || rel == null) continue;
            mappedIds.add(heir.getMemberId());

            // Check if this heir would be alive at time of deceased's death
            boolean wasAlive = heir.getDateOfDeath() == null ||
                    (deceased.getDateOfDeath() != null && heir.getDateOfDeath().after(deceased.getDateOfDeath()));
            String aliveTag = wasAlive ? "  ✓ INHERITS" : "  ✗ PRE-DECEASED";
            String lbl = buildLabel(heir) + "  — " + rel.getRelationName() + aliveTag;
            TreeItem<String> heirNode = new TreeItem<>(lbl);

            String cat = rel.getCategory();
            TreeItem<String> parent = catNodes.entrySet().stream()
                    .filter(e -> cat != null && cat.contains(e.getKey().split(" ")[0]))
                    .map(Map.Entry::getValue).findFirst()
                    .orElse(catNodes.get("Extended"));
            // More precise matching
            for (Map.Entry<String, TreeItem<String>> e : catNodes.entrySet()) {
                if (cat != null && cat.equals(e.getKey())) { parent = e.getValue(); break; }
            }
            parent.getChildren().add(heirNode);
        }

        catNodes.values().stream().filter(n -> !n.getChildren().isEmpty()).forEach(n -> root.getChildren().add(n));

        // Unmapped members
        TreeItem<String> unmapped = new TreeItem<>("Other Family (Not Mapped)");
        unmapped.setExpanded(true);
        for (FamilyMember m : db.getFamilyMembers()) {
            if (!mappedIds.contains(m.getMemberId()))
                unmapped.getChildren().add(new TreeItem<>(buildLabel(m)));
        }
        if (!unmapped.getChildren().isEmpty()) root.getChildren().add(unmapped);

        familyTree.setRoot(root);
        familyTree.setShowRoot(true);
    }

    // ===========================================================================
    // ACTION PANEL
    // ===========================================================================
    private void updateActionPanel() {
        if (selectedMember == null) return;
        FamilyMember m = selectedMember;
        FamilyMember deceased = db.getDeceased();
        boolean isPrincipal = deceased != null && m.getMemberId() == deceased.getMemberId();
        boolean isDead = m.getDateOfDeath() != null;

        selectedNodeLabel.setText(m.getName());
        
        String statusText = "Status: ";
        if (isPrincipal) statusText += "★ PRINCIPAL DECEASED — " + dateFormat.format(m.getDateOfDeath());
        else if (isDead) statusText += "✝ Died: " + dateFormat.format(m.getDateOfDeath());
        else statusText += "✓ Alive";
        statusLabel.setText(statusText);
        statusLabel.setStyle(isDead
                ? "-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 13px;"
                : "-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Relation info
        if (deceased != null && !isPrincipal) {
            DeceasedHeir mapping = db.getDeceasedHeirMappings().stream()
                    .filter(dh -> dh.getHeirId() == m.getMemberId()).findFirst().orElse(null);
            if (mapping != null) {
                RelationType rel = db.getRelationTypeById(mapping.getRelationId());
                boolean willInherit = !isDead || (deceased.getDateOfDeath() != null && m.getDateOfDeath().after(deceased.getDateOfDeath()));
                relationLabel.setText("Relation: " + (rel != null ? rel.getRelationName() + " (" + rel.getCategory() + ")" : "?") +
                        "\n" + (willInherit ? "→ Eligible to inherit (was alive at time of death)" : "→ NOT eligible (pre-deceased)"));
            } else {
                relationLabel.setText("Not mapped as heir — use quick-add buttons or the Heirs & Relations tab");
            }
        } else if (isPrincipal) {
            relationLabel.setText("This person's estate is being distributed.\nDOB: " + dateFormat.format(m.getDateOfBirth()));
        } else {
            relationLabel.setText("");
        }

        if (isDead) {
            toggleDeathBtn.setText("Mark as Alive");
            toggleDeathBtn.setStyle("-fx-background-color: rgba(34,197,94,0.2); -fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            toggleDeathBtn.setText("Mark as Deceased (set DOD)...");
            toggleDeathBtn.setStyle("-fx-background-color: rgba(239,68,68,0.2); -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    // ===========================================================================
    // TOGGLE DEATH — uses DatePicker so FaraidCalc date comparison works
    // ===========================================================================
    @FXML
    private void handleToggleDeath(ActionEvent event) {
        if (selectedMember == null) return;
        if (selectedMember.getDateOfDeath() != null) {
            // Mark alive
            selectedMember.setDateOfDeath(null);
        } else {
            // Ask for actual death date — critical for Faraid eligibility
            Dialog<Date> dialog = new Dialog<>();
            dialog.setTitle("Set Date of Death");
            FamilyMember deceased = db.getDeceased();
            String note = deceased != null
                    ? "If they died BEFORE " + dateFormat.format(deceased.getDateOfDeath()) + ", they will NOT inherit."
                    : "";
            dialog.setHeaderText("When did " + selectedMember.getName() + " pass away?\n" + note);
            ButtonType saveBtn = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

            DatePicker datePicker = new DatePicker(LocalDate.now());
            GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
            grid.add(new Label("Date of Death:"), 0, 0);
            grid.add(datePicker, 1, 0);
            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(btn -> {
                if (btn == saveBtn && datePicker.getValue() != null) {
                    return Date.from(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
                }
                return null;
            });

            dialog.showAndWait().ifPresent(dod -> selectedMember.setDateOfDeath(dod));
        }
        updateActionPanel();
        refreshTree();
    }

    @FXML
    private void handleRemoveNode(ActionEvent event) {
        if (selectedMember == null) return;
        if (selectedMember.getMemberId() == db.getPrincipalDeceasedId()) {
            new Alert(Alert.AlertType.WARNING, "Cannot remove the principal deceased.").showAndWait();
            return;
        }
        db.removeFamilyMember(selectedMember);
        selectedMember = null;
        actionPanel.setVisible(false);
        instructionLabel.setVisible(true);
        refreshTree();
    }

    // ===========================================================================
    // QUICK-ADD — collects name, gender, DOB, relation (schema-complete)
    // ===========================================================================
    @FXML private void handleAddSon(ActionEvent e)      { quickAdd("Son", 'M', 5); }
    @FXML private void handleAddDaughter(ActionEvent e)  { quickAdd("Daughter", 'F', 6); }
    @FXML private void handleAddWife(ActionEvent e)      { quickAdd("Wife", 'F', 2); }
    @FXML private void handleAddBrother(ActionEvent e)   { quickAdd("Full Brother", 'M', 12); }
    @FXML private void handleAddSister(ActionEvent e)    { quickAdd("Full Sister", 'F', 13); }
    @FXML private void handleAddFather(ActionEvent e)    { quickAdd("Father", 'M', 3); }
    @FXML private void handleAddMother(ActionEvent e)    { quickAdd("Mother", 'F', 4); }

    private void quickAdd(String defaultRole, char gender, int defaultRelationId) {
        FamilyMember deceased = db.getDeceased();
        if (deceased == null) {
            new Alert(Alert.AlertType.WARNING, "Set a principal deceased first.").showAndWait();
            return;
        }

        Dialog<FamilyMember> dialog = new Dialog<>();
        dialog.setTitle("Add " + defaultRole);
        dialog.setHeaderText("Adding " + defaultRole + " of " + deceased.getName());
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);

        TextField nameField = new TextField(); nameField.setPromptText("Full Name");
        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll("Male", "Female");
        genderBox.setValue(gender == 'M' ? "Male" : "Female");

        DatePicker dobPicker = new DatePicker(LocalDate.of(1990, 1, 1));

        ComboBox<RelationType> relationBox = new ComboBox<>();
        relationBox.getItems().addAll(db.getRelationTypes());
        relationBox.setConverter(new javafx.util.StringConverter<RelationType>() {
            @Override public String toString(RelationType r) {
                return r != null ? r.getRelationId() + ". " + r.getRelationName() + " (" + r.getCategory() + ")" : "";
            }
            @Override public RelationType fromString(String s) { return null; }
        });
        RelationType defaultRel = db.getRelationTypeById(defaultRelationId);
        if (defaultRel != null) relationBox.setValue(defaultRel);

        grid.add(new Label("Name:"), 0, 0);             grid.add(nameField, 1, 0);
        grid.add(new Label("Gender:"), 0, 1);            grid.add(genderBox, 1, 1);
        grid.add(new Label("Date of Birth:"), 0, 2);     grid.add(dobPicker, 1, 2);
        grid.add(new Label("Relation to Deceased:"), 0, 3); grid.add(relationBox, 1, 3);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(500);

        final RelationType[] chosenRelation = new RelationType[1];
        dialog.setResultConverter(dBtn -> {
            if (dBtn == saveBtn && !nameField.getText().trim().isEmpty() && dobPicker.getValue() != null) {
                char g = genderBox.getValue().equals("Male") ? 'M' : 'F';
                Date dob = Date.from(dobPicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
                Integer fatherId = null, motherId = null;
                chosenRelation[0] = relationBox.getValue();
                if (chosenRelation[0] != null && chosenRelation[0].getCategory() != null &&
                        chosenRelation[0].getCategory().contains("Descendant")) {
                    if (deceased.getGender() == 'M') fatherId = deceased.getMemberId();
                    else motherId = deceased.getMemberId();
                }
                return new FamilyMember(0, "", nameField.getText().trim(), dob, g, 0, null, fatherId, motherId);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newMember -> {
            db.addFamilyMember(newMember);
            if (chosenRelation[0] != null) {
                db.addDeceasedHeir(new DeceasedHeir(0, deceased.getMemberId(),
                        newMember.getMemberId(), chosenRelation[0].getRelationId()));
            }
            refreshTree();
        });
    }

    @FXML
    private void handleAddRelative(ActionEvent event) {
        quickAdd("Relative", 'M', 5);
    }
}
