package com.wirasat;

import com.wirasat.model.FamilyMember;
import com.wirasat.service.MockDataService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Shows estate summary SCOPED to the principal deceased.
 * Uses getDeceasedAssets/Liabilities/Wasiyats — mirrors queries.sql GetEstateState.
 */
public class DashboardController implements Initializable {

    @FXML private Label grossEstateLabel;
    @FXML private Label liabilitiesLabel;
    @FXML private Label wasiyatLabel;
    @FXML private Label netEstateLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MockDataService db = MockDataService.getInstance();
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "PK"));

        FamilyMember deceased = db.getDeceased();
        if (deceased == null) {
            grossEstateLabel.setText("No principal deceased set");
            liabilitiesLabel.setText("—");
            wasiyatLabel.setText("—");
            netEstateLabel.setText("—");
            return;
        }

        // Only deceased-owned assets (queries.sql: WHERE a.owner_id = target_deceased_id)
        BigDecimal gross = db.getDeceasedAssets().stream()
                .map(a -> a.getValue() != null ? a.getValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiab = db.getDeceasedLiabilities().stream()
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWasiyat = db.getDeceasedWasiyats().stream()
                .map(w -> w.getAmount() != null ? w.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Cap wasiyat at 1/3 of (gross - liabilities) per Islamic law
        BigDecimal available = gross.subtract(totalLiab);
        BigDecimal maxWasiyat = available.divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);
        if (totalWasiyat.compareTo(maxWasiyat) > 0) {
            totalWasiyat = maxWasiyat;
        }

        BigDecimal net = available.subtract(totalWasiyat);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;

        grossEstateLabel.setText(format.format(gross));
        liabilitiesLabel.setText(format.format(totalLiab));
        wasiyatLabel.setText(format.format(totalWasiyat));
        netEstateLabel.setText(format.format(net));
    }
}
