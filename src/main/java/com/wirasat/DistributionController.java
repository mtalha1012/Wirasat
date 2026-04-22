package com.wirasat;

import com.wirasat.model.*;
import com.wirasat.service.FaraidCalculationService;
import com.wirasat.service.MockDataService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Distribution engine. Calls FaraidCalculationService directly with data from MockDataService.
 * Shows transparent calculation breakdown: who was blocked, who was excluded as pre-deceased,
 * and the exact fractions assigned. No redundant logic — just passes data through.
 */
public class DistributionController implements Initializable {

    @FXML private Label resultSummary;
    @FXML private VBox heirBucketsContainer;
    @FXML private VBox assetCardsContainer;

    private MockDataService db;
    private FaraidCalculationService calcService;
    private NumberFormat format;

    private CalculationRun lastRun;
    private Map<Integer, BigDecimal> heirTargetValues = new LinkedHashMap<>();
    private Map<Integer, BigDecimal> heirPercentages = new LinkedHashMap<>();
    // assetId → Map<heirId, percentage of that asset>
    private Map<Integer, Map<Integer, BigDecimal>> assetAssignments = new LinkedHashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        db = MockDataService.getInstance();
        calcService = new FaraidCalculationService();
        format = NumberFormat.getCurrencyInstance(new Locale("en", "PK"));
    }

    @FXML
    private void handleRunCalculation(ActionEvent event) {
        FamilyMember deceased = db.getDeceased();
        if (deceased == null) {
            resultSummary.setText("⚠ No principal deceased set.");
            resultSummary.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            return;
        }

        try {
            // =================================================================
            // TRANSPARENT PIPELINE — call FaraidCalculationService directly
            // with ALL data from MockDataService scoped to the principal
            // =================================================================
            List<Asset> estateAssets = db.getDeceasedAssets();
            List<Liability> estateLiab = db.getDeceasedLiabilities();
            List<Wasiyat> estateWasiyat = db.getDeceasedWasiyats();
            List<DeceasedHeir> heirMappings = db.getDeceasedHeirMappings();

            lastRun = calcService.calculateFaraid(
                    deceased,
                    db.getFamilyMembers(),
                    heirMappings,
                    db.getRelationTypes(),
                    db.getBlockingRules(),
                    db.getShareRules(),
                    estateAssets,
                    estateLiab,
                    estateWasiyat
            );

            // Build transparent summary
            StringBuilder sb = new StringBuilder();
            String netStr = lastRun.getNetEstate() != null ? format.format(lastRun.getNetEstate()) : "Rs 0";
            sb.append("✓ Net Estate: ").append(netStr);
            sb.append("\nHeir Mappings: ").append(heirMappings.size());
            sb.append(" → Eligible (alive + unblocked): ").append(lastRun.getAllocations().size());

            // Show who was excluded
            Set<Integer> allocatedIds = lastRun.getAllocations().stream()
                    .map(AssetAllocation::getHeirId).collect(Collectors.toSet());
            for (DeceasedHeir dh : heirMappings) {
                if (!allocatedIds.contains(dh.getHeirId())) {
                    FamilyMember m = db.getMemberById(dh.getHeirId());
                    RelationType r = db.getRelationTypeById(dh.getRelationId());
                    String reason = "blocked/excluded";
                    if (m != null && m.getDateOfDeath() != null &&
                            deceased.getDateOfDeath() != null &&
                            !m.getDateOfDeath().after(deceased.getDateOfDeath())) {
                        reason = "pre-deceased (DOD before principal)";
                    } else {
                        reason = "blocked by higher-priority heir";
                    }
                    sb.append("\n  ✗ ").append(m != null ? m.getName() : "?")
                            .append(" [").append(r != null ? r.getRelationName() : "?").append("] — ").append(reason);
                }
            }

            resultSummary.setText(sb.toString());
            resultSummary.setStyle("-fx-font-size: 13px; -fx-text-fill: #22c55e; -fx-font-weight: bold;");
            resultSummary.setWrapText(true);

            // Populate heir data maps
            heirTargetValues.clear();
            heirPercentages.clear();
            assetAssignments.clear();
            for (AssetAllocation a : lastRun.getAllocations()) {
                heirTargetValues.put(a.getHeirId(), a.getAllocatedValue() != null ? a.getAllocatedValue() : BigDecimal.ZERO);
                heirPercentages.put(a.getHeirId(), a.getAllocatedPercentage() != null ? a.getAllocatedPercentage() : BigDecimal.ZERO);
            }

            updateUI();

        } catch (Exception e) {
            resultSummary.setText("⚠ Error: " + e.getMessage());
            resultSummary.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444;");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAutoAssign(ActionEvent event) {
        if (lastRun == null || lastRun.getAllocations().isEmpty()) {
            resultSummary.setText("⚠ Run calculation first.");
            return;
        }

        assetAssignments.clear();
        List<Asset> estateAssets = db.getDeceasedAssets();

        // Track running assigned totals
        Map<Integer, BigDecimal> running = new LinkedHashMap<>();
        heirTargetValues.keySet().forEach(id -> running.put(id, BigDecimal.ZERO));

        // Phase 1: Non-shareable → best-fit bin packing (whole to one heir)
        List<Asset> nonShareable = estateAssets.stream()
                .filter(a -> !a.isShareable())
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        for (Asset asset : nonShareable) {
            int bestHeir = -1;
            BigDecimal bestGap = null;
            for (Map.Entry<Integer, BigDecimal> e : heirTargetValues.entrySet()) {
                BigDecimal gap = e.getValue().subtract(running.getOrDefault(e.getKey(), BigDecimal.ZERO));
                if (gap.compareTo(asset.getValue()) >= 0) {
                    if (bestGap == null || gap.compareTo(bestGap) < 0) {
                        bestGap = gap;
                        bestHeir = e.getKey();
                    }
                }
            }
            if (bestHeir == -1) { // fallback: largest gap
                BigDecimal maxGap = BigDecimal.ZERO;
                for (Map.Entry<Integer, BigDecimal> e : heirTargetValues.entrySet()) {
                    BigDecimal gap = e.getValue().subtract(running.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    if (gap.compareTo(maxGap) > 0) { maxGap = gap; bestHeir = e.getKey(); }
                }
            }
            if (bestHeir != -1) {
                Map<Integer, BigDecimal> splits = new LinkedHashMap<>();
                splits.put(bestHeir, BigDecimal.valueOf(100));
                assetAssignments.put(asset.getAssetId(), splits);
                running.put(bestHeir, running.get(bestHeir).add(asset.getValue()));
            }
        }

        // Phase 2: Shareable → fill remaining GAPS after non-shareables
        // Calculate how much each heir still needs
        Map<Integer, BigDecimal> gaps = new LinkedHashMap<>();
        BigDecimal totalGap = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> e : heirTargetValues.entrySet()) {
            BigDecimal gap = e.getValue().subtract(running.getOrDefault(e.getKey(), BigDecimal.ZERO));
            if (gap.compareTo(BigDecimal.ZERO) < 0) gap = BigDecimal.ZERO;
            gaps.put(e.getKey(), gap);
            totalGap = totalGap.add(gap);
        }

        List<Asset> shareable = estateAssets.stream()
                .filter(Asset::isShareable)
                .collect(Collectors.toList());

        for (Asset asset : shareable) {
            Map<Integer, BigDecimal> splits = new LinkedHashMap<>();
            if (totalGap.compareTo(BigDecimal.ZERO) > 0) {
                // Split this asset in proportion to each heir's remaining gap
                for (Map.Entry<Integer, BigDecimal> e : gaps.entrySet()) {
                    if (e.getValue().compareTo(BigDecimal.ZERO) > 0) {
                        // This heir's share of THIS asset = (their gap / total gap) * 100%
                        BigDecimal pct = e.getValue()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(totalGap, 4, RoundingMode.HALF_UP);
                        splits.put(e.getKey(), pct.setScale(2, RoundingMode.HALF_UP));
                    }
                }
            }
            assetAssignments.put(asset.getAssetId(), splits);
            // Update running totals and gaps for next shareable asset
            for (Map.Entry<Integer, BigDecimal> s : splits.entrySet()) {
                BigDecimal portion = asset.getValue().multiply(s.getValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                running.merge(s.getKey(), portion, BigDecimal::add);
            }
            // Recalculate gaps for next shareable
            totalGap = BigDecimal.ZERO;
            for (Map.Entry<Integer, BigDecimal> e : heirTargetValues.entrySet()) {
                BigDecimal gap = e.getValue().subtract(running.getOrDefault(e.getKey(), BigDecimal.ZERO));
                if (gap.compareTo(BigDecimal.ZERO) < 0) gap = BigDecimal.ZERO;
                gaps.put(e.getKey(), gap);
                totalGap = totalGap.add(gap);
            }
        }

        updateUI();
    }

    // ===========================================================================
    // COMPUTE
    // ===========================================================================
    private Map<Integer, BigDecimal> computeAssignedTotals() {
        Map<Integer, BigDecimal> totals = new LinkedHashMap<>();
        heirTargetValues.keySet().forEach(id -> totals.put(id, BigDecimal.ZERO));
        for (Map.Entry<Integer, Map<Integer, BigDecimal>> entry : assetAssignments.entrySet()) {
            Asset asset = db.getDeceasedAssets().stream()
                    .filter(a -> a.getAssetId() == entry.getKey()).findFirst().orElse(null);
            if (asset == null) continue;
            for (Map.Entry<Integer, BigDecimal> split : entry.getValue().entrySet()) {
                BigDecimal portion = asset.getValue().multiply(split.getValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                totals.merge(split.getKey(), portion, BigDecimal::add);
            }
        }
        return totals;
    }

    // ===========================================================================
    // UI
    // ===========================================================================
    private void updateUI() {
        Map<Integer, BigDecimal> assigned = computeAssignedTotals();

        // LEFT: Heir progress bars
        heirBucketsContainer.getChildren().clear();
        for (AssetAllocation alloc : lastRun.getAllocations()) {
            int hId = alloc.getHeirId();
            FamilyMember heir = db.getMemberById(hId);
            BigDecimal target = heirTargetValues.getOrDefault(hId, BigDecimal.ZERO);
            BigDecimal current = assigned.getOrDefault(hId, BigDecimal.ZERO);
            BigDecimal pct = heirPercentages.getOrDefault(hId, BigDecimal.ZERO);
            double progress = target.compareTo(BigDecimal.ZERO) == 0 ? 0 : current.doubleValue() / target.doubleValue();

            // Find relation name for this heir
            DeceasedHeir dh = db.getDeceasedHeirMappings().stream()
                    .filter(d -> d.getHeirId() == hId).findFirst().orElse(null);
            RelationType rel = dh != null ? db.getRelationTypeById(dh.getRelationId()) : null;
            String relName = rel != null ? rel.getRelationName() : "";

            VBox box = new VBox(6);
            box.setPadding(new Insets(12));
            box.setStyle("-fx-border-color: #374151; -fx-border-radius: 8; -fx-background-color: #1c2331; -fx-background-radius: 8;");

            Label nameLbl = new Label((heir != null ? heir.getName() : "ID " + hId) + "  (" + relName + ")");
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: white;");

            Label pctLbl = new Label("Faraid Share: " + pct.setScale(2, RoundingMode.HALF_UP) + "% = " + format.format(target));
            pctLbl.setStyle("-fx-text-fill: #E2B93B; -fx-font-size: 12px;");

            ProgressBar pBar = new ProgressBar(Math.min(progress, 1.0));
            pBar.setMaxWidth(Double.MAX_VALUE);
            if (progress > 1.05) {
                pBar.setStyle("-fx-accent: #ef4444;");
                box.setStyle("-fx-border-color: #ef4444; -fx-border-radius: 8; -fx-background-color: rgba(239,68,68,0.1); -fx-background-radius: 8;");
            } else if (progress > 0.95) {
                pBar.setStyle("-fx-accent: #22c55e;");
                box.setStyle("-fx-border-color: #22c55e; -fx-border-radius: 8; -fx-background-color: rgba(34,197,94,0.1); -fx-background-radius: 8;");
            }

            Label fillLbl = new Label("Assigned: " + format.format(current) + " [" + String.format("%.1f%%", progress * 100) + " filled]");
            fillLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");

            // Asset list
            List<String> items = new ArrayList<>();
            for (Map.Entry<Integer, Map<Integer, BigDecimal>> ae : assetAssignments.entrySet()) {
                BigDecimal hp = ae.getValue().getOrDefault(hId, BigDecimal.ZERO);
                if (hp.compareTo(BigDecimal.ZERO) > 0) {
                    Asset a = db.getDeceasedAssets().stream().filter(x -> x.getAssetId() == ae.getKey()).findFirst().orElse(null);
                    if (a != null) items.add("  • " + a.getAssetName() + " (" + hp.setScale(1, RoundingMode.HALF_UP) + "%)");
                }
            }

            box.getChildren().addAll(nameLbl, pctLbl, pBar, fillLbl);
            if (!items.isEmpty()) {
                Label list = new Label(String.join("\n", items));
                list.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 11px;");
                list.setWrapText(true);
                box.getChildren().add(list);
            }
            heirBucketsContainer.getChildren().add(box);
        }

        // RIGHT: Asset cards
        assetCardsContainer.getChildren().clear();
        List<FamilyMember> eligibleHeirs = db.getFamilyMembers().stream()
                .filter(m -> heirTargetValues.containsKey(m.getMemberId())).collect(Collectors.toList());

        for (Asset asset : db.getDeceasedAssets()) {
            VBox box = new VBox(8);
            box.setPadding(new Insets(12));
            boolean isSh = asset.isShareable();
            String bc = isSh ? "#22c55e" : "#E2B93B";
            String bg = isSh ? "rgba(34,197,94,0.1)" : "rgba(226,185,59,0.1)";
            box.setStyle("-fx-padding: 12; -fx-border-color: " + bc + "; -fx-background-color: " + bg + "; -fx-border-radius: 8; -fx-background-radius: 8;");

            HBox titleRow = new HBox(10);
            Label aName = new Label(asset.getAssetName());
            aName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: white;");
            aName.setWrapText(true);
            HBox.setHgrow(aName, Priority.ALWAYS);
            Label tag = new Label(isSh ? "SHAREABLE" : "NON-SHAREABLE");
            tag.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 4; " +
                    (isSh ? "-fx-background-color: rgba(34,197,94,0.2); -fx-text-fill: #34d399;"
                            : "-fx-background-color: rgba(226,185,59,0.2); -fx-text-fill: #E2B93B;"));
            titleRow.getChildren().addAll(aName, tag);

            Label valLbl = new Label(format.format(asset.getValue()));
            valLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

            box.getChildren().addAll(titleRow, valLbl);

            if (isSh) {
                // Shareable: show proportional split
                Map<Integer, BigDecimal> splits = assetAssignments.getOrDefault(asset.getAssetId(), Collections.emptyMap());
                if (!splits.isEmpty()) {
                    for (Map.Entry<Integer, BigDecimal> sp : splits.entrySet()) {
                        FamilyMember h = db.getMemberById(sp.getKey());
                        BigDecimal portion = asset.getValue().multiply(sp.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        Label l = new Label("  • " + (h != null ? h.getName() : "?") + ": " +
                                sp.getValue().setScale(1, RoundingMode.HALF_UP) + "% = " + format.format(portion));
                        l.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 11px;");
                        box.getChildren().add(l);
                    }
                } else {
                    Label n = new Label("Run Auto-Assign to split across heirs");
                    n.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
                    box.getChildren().add(n);
                }
            } else {
                // Non-shareable: ComboBox
                HBox cc = new HBox(8); cc.setAlignment(Pos.CENTER_LEFT);
                Label l = new Label("Assign to:"); l.setStyle("-fx-text-fill: #9ca3af;");
                ComboBox<String> hc = new ComboBox<>();
                hc.getItems().add("— UNASSIGNED —");
                for (FamilyMember tm : eligibleHeirs) {
                    BigDecimal p = heirPercentages.getOrDefault(tm.getMemberId(), BigDecimal.ZERO);
                    hc.getItems().add(tm.getMemberId() + " : " + tm.getName() + " (" + p.setScale(1, RoundingMode.HALF_UP) + "%)");
                }
                hc.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(hc, Priority.ALWAYS);

                Map<Integer, BigDecimal> curSplit = assetAssignments.getOrDefault(asset.getAssetId(), Collections.emptyMap());
                Optional<Map.Entry<Integer, BigDecimal>> cur = curSplit.entrySet().stream().findFirst();
                if (cur.isPresent()) {
                    FamilyMember ch = db.getMemberById(cur.get().getKey());
                    if (ch != null) {
                        BigDecimal p = heirPercentages.getOrDefault(ch.getMemberId(), BigDecimal.ZERO);
                        hc.setValue(ch.getMemberId() + " : " + ch.getName() + " (" + p.setScale(1, RoundingMode.HALF_UP) + "%)");
                    }
                } else {
                    hc.setValue("— UNASSIGNED —");
                }

                hc.setOnAction(ev -> {
                    String val = hc.getValue();
                    if (val == null || val.startsWith("—")) {
                        assetAssignments.remove(asset.getAssetId());
                    } else {
                        int newHeir = Integer.parseInt(val.split(" : ")[0].trim());
                        Map<Integer, BigDecimal> sp = new LinkedHashMap<>();
                        sp.put(newHeir, BigDecimal.valueOf(100));
                        assetAssignments.put(asset.getAssetId(), sp);
                    }
                    updateUI();
                });
                cc.getChildren().addAll(l, hc);
                box.getChildren().add(cc);
            }
            assetCardsContainer.getChildren().add(box);
        }
    }
}
