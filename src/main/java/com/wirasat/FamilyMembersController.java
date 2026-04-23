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
    @FXML private Button setPrincipalBtn;
    @FXML private Button addSpouseBtn;
    @FXML private Button editMemberBtn;

    private MockDataService db;
    private FamilyMember selectedMember;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

    @FXML
    private void handleSetPrincipal(ActionEvent event) {
        if (selectedMember == null) return;
        if (selectedMember.getDateOfDeath() == null) {
            new Alert(Alert.AlertType.WARNING, "This person is marked as Alive. Mark them as Deceased first before making them the Principal Deceased.").showAndWait();
            return;
        }
        db.setPrincipalDeceasedId(selectedMember.getMemberId());
        
        // Notify user about context shift
        Alert info = new Alert(Alert.AlertType.INFORMATION, "Principal Deceased is now " + selectedMember.getName() + ".\nThe inheritance dashboard and tools will now calculate distributions based on their estate.");
        info.setHeaderText("Distribution Context Changed");
        info.getDialogPane().getStylesheets().add(getClass().getResource("/com/wirasat/styles.css").toExternalForm());
        info.getDialogPane().getStyleClass().add("dark-panel");
        info.showAndWait();
        
        updateActionPanel();
        refreshTree();
    }

    @FXML
    private void handleEditMember(ActionEvent event) {
        if (selectedMember == null) return;
        editMemberDialog(selectedMember);
    }

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

        // Smart UI logic for Spouse
        if (deceased != null) {
            if (deceased.getGender() == 'F') {
                addSpouseBtn.setText("+ Husband");
                addSpouseBtn.setOnAction(e -> quickAdd("Husband", 'M', 1));
            } else {
                addSpouseBtn.setText("+ Wife");
                addSpouseBtn.setOnAction(e -> quickAdd("Wife", 'F', 2));
            }
        }

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

        if (isPrincipal) {
            setPrincipalBtn.setVisible(false);
            setPrincipalBtn.setManaged(false);
        } else {
            setPrincipalBtn.setVisible(true);
            setPrincipalBtn.setManaged(true);
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
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/wirasat/styles.css").toExternalForm());
            dialog.getDialogPane().getStyleClass().add("dark-panel");
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
    // QUICK-ADD & ROBUST EDIT — honors schema fields
    // ===========================================================================
    private void editMemberDialog(FamilyMember m) {
        FamilyMember deceased = db.getDeceased();

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Member Details");
        dialog.setHeaderText("Modifying " + m.getName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/wirasat/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dark-panel");
        ButtonType saveBtn = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);

        TextField nameField = new TextField(m.getName());
        TextField cnicField = new TextField(m.getCnic() != null ? m.getCnic() : "");
        cnicField.setPromptText("e.g. 35202-1234567-1");
        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll("Male", "Female");
        genderBox.setValue(m.getGender() == 'M' ? "Male" : "Female");

        DatePicker dobPicker = new DatePicker(m.getDateOfBirth() != null ? m.getDateOfBirth().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : LocalDate.of(1990, 1, 1));

        // Setup relation combo box
        ComboBox<RelationType> relationBox = new ComboBox<>();
        relationBox.getItems().add(null); // Allow explicitly removing mapping
        relationBox.getItems().addAll(db.getRelationTypes());
        relationBox.setConverter(new javafx.util.StringConverter<RelationType>() {
            @Override public String toString(RelationType r) {
                return r != null ? r.getRelationId() + ". " + r.getRelationName() + " (" + r.getCategory() + ")" : "Not related (Remove Mapping)";
            }
            @Override public RelationType fromString(String s) { return null; }
        });
        
        // Find existing mapping
        DeceasedHeir mapping = null;
        if (deceased != null) {
            mapping = db.getDeceasedHeirMappings().stream()
                .filter(dh -> dh.getHeirId() == m.getMemberId()).findFirst().orElse(null);
            if (mapping != null) {
                relationBox.setValue(db.getRelationTypeById(mapping.getRelationId()));
            } else {
                relationBox.setValue(null);
            }
        }

        // Setup explicit father and mother combos mapped purely to family_members table
        ComboBox<FamilyMember> fatherBox = new ComboBox<>();
        ComboBox<FamilyMember> motherBox = new ComboBox<>();
        fatherBox.getItems().add(null);
        motherBox.getItems().add(null);
        for (FamilyMember potentialParent : db.getFamilyMembers()) {
            if (potentialParent.getMemberId() == m.getMemberId()) continue; // Cannot be own parent
            if (potentialParent.getGender() == 'M') fatherBox.getItems().add(potentialParent);
            else motherBox.getItems().add(potentialParent);
        }
        
        javafx.util.StringConverter<FamilyMember> parentConverter = new javafx.util.StringConverter<FamilyMember>() {
            @Override public String toString(FamilyMember fm) { return fm != null ? fm.getName() : "None/Unknown"; }
            @Override public FamilyMember fromString(String s) { return null; }
        };
        fatherBox.setConverter(parentConverter);
        motherBox.setConverter(parentConverter);
        
        fatherBox.setValue(m.getFatherId() != null ? db.getMemberById(m.getFatherId()) : null);
        motherBox.setValue(m.getMotherId() != null ? db.getMemberById(m.getMotherId()) : null);

        grid.add(new Label("Name:"), 0, 0);             grid.add(nameField, 1, 0);
        grid.add(new Label("CNIC:"), 0, 1);              grid.add(cnicField, 1, 1);
        grid.add(new Label("Gender:"), 0, 2);             grid.add(genderBox, 1, 2);
        grid.add(new Label("Date of Birth:"), 0, 3);      grid.add(dobPicker, 1, 3);
        
        boolean isPrincipal = deceased != null && m.getMemberId() == deceased.getMemberId();
        if (!isPrincipal) {
            grid.add(new Label("Relation to Principal:"), 0, 4); grid.add(relationBox, 1, 4);
            grid.add(new Label("Father:"), 0, 5); grid.add(fatherBox, 1, 5);
            grid.add(new Label("Mother:"), 0, 6); grid.add(motherBox, 1, 6);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(500);

        final DeceasedHeir existingMap = mapping;
        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                m.setName(nameField.getText().trim());
                String cnic = cnicField.getText().trim();
                m.setCnic(cnic.isEmpty() ? null : cnic);
                m.setGender(genderBox.getValue().equals("Male") ? 'M' : 'F');
                if (dobPicker.getValue() != null) {
                    m.setDateOfBirth(Date.from(dobPicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }
                
                if (!isPrincipal) {
                    if (fatherBox.getValue() != null) m.setFatherId(fatherBox.getValue().getMemberId());
                    else m.setFatherId(null);
                    
                    if (motherBox.getValue() != null) m.setMotherId(motherBox.getValue().getMemberId());
                    else m.setMotherId(null);

                    RelationType rel = relationBox.getValue();
                    if (rel != null) {
                        if (existingMap != null) existingMap.setRelationId(rel.getRelationId());
                        else db.addDeceasedHeir(new DeceasedHeir(0, deceased.getMemberId(), m.getMemberId(), rel.getRelationId()));
                    } else if (existingMap != null) {
                        db.removeDeceasedHeir(existingMap);
                    }
                }
                return true;
            }
            return false;
        });

        dialog.showAndWait().ifPresent(changed -> {
            if (changed) {
                updateActionPanel();
                refreshTree();
            }
        });
    }

    @FXML private void handleAddSon(ActionEvent e)      { quickAdd("Son", 'M', 5); }
    @FXML private void handleAddDaughter(ActionEvent e)  { quickAdd("Daughter", 'F', 6); }
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
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/wirasat/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dark-panel");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);

        TextField nameField = new TextField(); nameField.setPromptText("Full Name");
        TextField cnicField = new TextField(); cnicField.setPromptText("e.g. 35202-1234567-1");
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
        grid.add(new Label("CNIC:"), 0, 1);              grid.add(cnicField, 1, 1);
        grid.add(new Label("Gender:"), 0, 2);             grid.add(genderBox, 1, 2);
        grid.add(new Label("Date of Birth:"), 0, 3);      grid.add(dobPicker, 1, 3);
        grid.add(new Label("Relation to Deceased:"), 0, 4); grid.add(relationBox, 1, 4);
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
                return new FamilyMember(0, cnicField.getText().trim().isEmpty() ? null : cnicField.getText().trim(), nameField.getText().trim(), dob, g, null, fatherId, motherId);
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
