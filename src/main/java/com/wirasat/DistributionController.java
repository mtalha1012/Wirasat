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
        
        handleRunCalculation(null);
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

            // Build transparent summary showing the math working
            BigDecimal gross = estateAssets.stream()
                    .map(a -> a.getValue() != null ? a.getValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalLiab = estateLiab.stream()
                    .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalWasiyat = estateWasiyat.stream()
                    .map(w -> w.getAmount() != null ? w.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal maxWasiyat = gross.subtract(totalLiab).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            BigDecimal executedWasiyat = totalWasiyat.compareTo(maxWasiyat) > 0 ? maxWasiyat : totalWasiyat;

            StringBuilder sb = new StringBuilder();
            sb.append("Estate Math: Gross ").append(format.format(gross))
              .append(" - Liab ").append(format.format(totalLiab))
              .append(" - Will ").append(format.format(executedWasiyat));
            if (totalWasiyat.compareTo(maxWasiyat) > 0) sb.append(" (capped)");

            String netStr = lastRun.getNetEstate() != null ? format.format(lastRun.getNetEstate()) : "Rs 0";
            sb.append("\n✓ Net Estate: ").append(netStr);
            sb.append("   → Total Heirs Mapped: ").append(heirMappings.size());
            sb.append("   → Eligible (alive & unblocked): ").append(lastRun.getAllocations().size());

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

            if (gross.compareTo(BigDecimal.ZERO) > 0) {
                if (totalLiab.compareTo(BigDecimal.ZERO) > 0) {
                    heirTargetValues.put(-1, totalLiab);
                    heirPercentages.put(-1, totalLiab.multiply(BigDecimal.valueOf(100)).divide(gross, 4, RoundingMode.HALF_UP));
                }
                if (executedWasiyat.compareTo(BigDecimal.ZERO) > 0) {
                    heirTargetValues.put(-2, executedWasiyat);
                    heirPercentages.put(-2, executedWasiyat.multiply(BigDecimal.valueOf(100)).divide(gross, 4, RoundingMode.HALF_UP));
                }
            }

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

        // Phase 1: Try to fit ANY asset (shareable or not) 100% whole into a single gap
        List<Asset> sortedAssets = estateAssets.stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        List<Asset> remainingAssets = new ArrayList<>();

        for (Asset asset : sortedAssets) {
            int bestHeir = -1;
            BigDecimal bestGap = null;
            // Best fit: find the smallest gap that is >= asset.getValue()
            for (Map.Entry<Integer, BigDecimal> e : heirTargetValues.entrySet()) {
                BigDecimal gap = e.getValue().subtract(running.getOrDefault(e.getKey(), BigDecimal.ZERO));
                if (gap.compareTo(asset.getValue()) >= 0) {
                    if (bestGap == null || gap.compareTo(bestGap) < 0) {
                        bestGap = gap;
                        bestHeir = e.getKey();
                    }
                }
            }
            if (bestHeir != -1) {
                Map<Integer, BigDecimal> splits = new LinkedHashMap<>();
                splits.put(bestHeir, BigDecimal.valueOf(100));
                assetAssignments.put(asset.getAssetId(), splits);
                running.put(bestHeir, running.get(bestHeir).add(asset.getValue()));
            } else {
                remainingAssets.add(asset);
            }
        }

        // Phase 2: Greedy Waterfall strategy for remaining assets (which are larger than any single remaining gap)
        // We fill the largest gaps first until the asset is exhausted. This avoids shattering assets into tiny chunks.
        for (Asset asset : remainingAssets) {
            Map<Integer, BigDecimal> splits = new LinkedHashMap<>();
            BigDecimal assetRemaining = asset.getValue();
            
            // Sort remaining gaps descending
            List<Integer> gapQueue = new ArrayList<>(heirTargetValues.keySet());
            gapQueue.sort((h1, h2) -> {
                BigDecimal g1 = heirTargetValues.get(h1).subtract(running.getOrDefault(h1, BigDecimal.ZERO));
                BigDecimal g2 = heirTargetValues.get(h2).subtract(running.getOrDefault(h2, BigDecimal.ZERO));
                return g2.compareTo(g1);
            });

            for (Integer hId : gapQueue) {
                if (assetRemaining.compareTo(BigDecimal.ZERO) <= 0) break;
                
                BigDecimal gap = heirTargetValues.get(hId).subtract(running.getOrDefault(hId, BigDecimal.ZERO));
                if (gap.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal amountToTake = gap.min(assetRemaining);
                BigDecimal pctToTake = amountToTake.multiply(BigDecimal.valueOf(100)).divide(asset.getValue(), 4, RoundingMode.HALF_UP);
                
                splits.put(hId, pctToTake.setScale(2, RoundingMode.HALF_UP));
                assetRemaining = assetRemaining.subtract(amountToTake);
                
                // Track exact amount taken
                BigDecimal exactAssigned = asset.getValue().multiply(splits.get(hId)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                running.put(hId, running.get(hId).add(exactAssigned));
            }
            
            if (!splits.isEmpty()) {
                assetAssignments.put(asset.getAssetId(), splits);
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
        
        List<Integer> bucketIds = new ArrayList<>();
        if (heirTargetValues.containsKey(-1)) bucketIds.add(-1);
        if (heirTargetValues.containsKey(-2)) bucketIds.add(-2);
        for (AssetAllocation alloc : lastRun.getAllocations()) {
            bucketIds.add(alloc.getHeirId());
        }

        for (int hId : bucketIds) {
            BigDecimal target = heirTargetValues.getOrDefault(hId, BigDecimal.ZERO);
            BigDecimal current = assigned.getOrDefault(hId, BigDecimal.ZERO);
            BigDecimal pct = heirPercentages.getOrDefault(hId, BigDecimal.ZERO);
            double progress = target.compareTo(BigDecimal.ZERO) == 0 ? 0 : current.doubleValue() / target.doubleValue();

            VBox box = new VBox(6);
            box.setPadding(new Insets(12));
            box.setStyle("-fx-border-color: #374151; -fx-border-radius: 8; -fx-background-color: #1c2331; -fx-background-radius: 8;");

            String nameText;
            String headerStyle;
            String pctLabelText;

            if (hId == -1) {
                nameText = "Liabilities (Debt)";
                headerStyle = "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #ef4444;";
                pctLabelText = "Obligation: " + format.format(target) + " (" + pct.setScale(2, RoundingMode.HALF_UP) + "% of Gross)";
            } else if (hId == -2) {
                nameText = "Wasiyat (Will)";
                headerStyle = "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #f59e0b;";
                pctLabelText = "Obligation: " + format.format(target) + " (" + pct.setScale(2, RoundingMode.HALF_UP) + "% of Gross)";
            } else {
                FamilyMember heir = db.getMemberById(hId);
                DeceasedHeir dh = db.getDeceasedHeirMappings().stream()
                        .filter(d -> d.getHeirId() == hId).findFirst().orElse(null);
                RelationType rel = dh != null ? db.getRelationTypeById(dh.getRelationId()) : null;
                String relName = rel != null ? rel.getRelationName() : "";
                nameText = (heir != null ? heir.getName() : "ID " + hId) + "  (" + relName + ")";
                headerStyle = "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: white;";
                pctLabelText = "Faraid Share: " + pct.setScale(2, RoundingMode.HALF_UP) + "% = " + format.format(target);
            }

            Label nameLbl = new Label(nameText);
            nameLbl.setStyle(headerStyle);

            Label pctLbl = new Label(pctLabelText);
            pctLbl.setStyle(hId < 0 ? "-fx-text-fill: #9ca3af; -fx-font-size: 12px;" : "-fx-text-fill: #E2B93B; -fx-font-size: 12px;");

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

            Map<Integer, BigDecimal> curSplit = assetAssignments.getOrDefault(asset.getAssetId(), Collections.emptyMap());
            boolean isSplit = curSplit.size() > 1;

            if (isSh) {
                // Shareable: always treated as proportional
                if (curSplit.isEmpty()) {
                    Label n = new Label("Run Auto-Assign to split across heirs");
                    n.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
                    box.getChildren().add(n);
                }
            } else {
                // Non-shareable: Provide ComboBox for manual override
                HBox cc = new HBox(8); cc.setAlignment(Pos.CENTER_LEFT);
                Label l = new Label("Assign to:"); l.setStyle("-fx-text-fill: #9ca3af;");
                ComboBox<String> hc = new ComboBox<>();
                hc.getItems().add("— UNASSIGNED —");
                
                // Add obligations first if they exist
                if (heirTargetValues.containsKey(-1)) {
                    hc.getItems().add("-1 : Liabilities (Debt)");
                }
                if (heirTargetValues.containsKey(-2)) {
                    hc.getItems().add("-2 : Wasiyat (Will)");
                }
                
                for (FamilyMember tm : eligibleHeirs) {
                    BigDecimal p = heirPercentages.getOrDefault(tm.getMemberId(), BigDecimal.ZERO);
                    hc.getItems().add(tm.getMemberId() + " : " + tm.getName() + " (" + p.setScale(1, RoundingMode.HALF_UP) + "%)");
                }
                hc.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(hc, Priority.ALWAYS);

                if (curSplit.size() == 1) {
                    int chId = curSplit.entrySet().iterator().next().getKey();
                    if (chId == -1) {
                        hc.setValue("-1 : Liabilities (Debt)");
                    } else if (chId == -2) {
                        hc.setValue("-2 : Wasiyat (Will)");
                    } else {
                        FamilyMember ch = db.getMemberById(chId);
                        if (ch != null) {
                            BigDecimal p = heirPercentages.getOrDefault(ch.getMemberId(), BigDecimal.ZERO);
                            hc.setValue(ch.getMemberId() + " : " + ch.getName() + " (" + p.setScale(1, RoundingMode.HALF_UP) + "%)");
                        }
                    }
                } else if (isSplit) {
                    hc.getItems().add("— JOINTLY SHARED (AUTO) —");
                    hc.setValue("— JOINTLY SHARED (AUTO) —");
                } else {
                    hc.setValue("— UNASSIGNED —");
                }

                hc.setOnAction(ev -> {
                    String val = hc.getValue();
                    if (val == null || val.startsWith("— UN") || val.startsWith("— JOINT")) {
                        if (val != null && val.startsWith("— UN")) assetAssignments.remove(asset.getAssetId());
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

            // Print the breakdown for BOTH shareable and split unshareables
            if (!curSplit.isEmpty() && (isSh || isSplit)) {
                for (Map.Entry<Integer, BigDecimal> sp : curSplit.entrySet()) {
                    int spId = sp.getKey();
                    String hName;
                    if (spId == -1) hName = "Liabilities";
                    else if (spId == -2) hName = "Wasiyat";
                    else {
                        FamilyMember h = db.getMemberById(spId);
                        hName = h != null ? h.getName() : "?";
                    }
                    BigDecimal portion = asset.getValue().multiply(sp.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    Label lb = new Label("  • " + hName + ": " +
                            sp.getValue().setScale(2, RoundingMode.HALF_UP) + "% = " + format.format(portion));
                    lb.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 11px;");
                    box.getChildren().add(lb);
                }
            }
            assetCardsContainer.getChildren().add(box);
        }
    }
}
