package com.wirasat.model;

import java.math.BigDecimal;
import java.util.Date;

public class ValuationHistory {
    private int valuationHistoryId;
    private int assetId;
    private Date valuationDate;
    private BigDecimal amount;

    public ValuationHistory() {
    }

    public ValuationHistory(int valuationHistoryId, int assetId, Date valuationDate, BigDecimal amount) {
        this.valuationHistoryId = valuationHistoryId;
        this.assetId = assetId;
        this.valuationDate = valuationDate;
        this.amount = amount;
    }

    public int getValuationHistoryId() {
        return valuationHistoryId;
    }

    public void setValuationHistoryId(int valuationHistoryId) {
        this.valuationHistoryId = valuationHistoryId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public Date getValuationDate() {
        return valuationDate;
    }

    public void setValuationDate(Date valuationDate) {
        this.valuationDate = valuationDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "ValuationHistory{" +
                "valuationHistoryId=" + valuationHistoryId +
                ", assetId=" + assetId +
                ", valuationDate=" + valuationDate +
                ", amount=" + amount +
                '}';
    }
}
