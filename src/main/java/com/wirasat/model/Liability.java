package com.wirasat.model;

import java.math.BigDecimal;

public class Liability {
    private int liabilityId;
    private String details;
    private BigDecimal amount;
    private int deceasedId;

    public Liability() {
    }

    public Liability(int liabilityId, String details, BigDecimal amount, int deceasedId) {
        this.liabilityId = liabilityId;
        this.details = details;
        this.amount = amount;
        this.deceasedId = deceasedId;
    }

    public int getLiabilityId() {
        return liabilityId;
    }

    public void setLiabilityId(int liabilityId) {
        this.liabilityId = liabilityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getDeceasedId() {
        return deceasedId;
    }

    public void setDeceasedId(int deceasedId) {
        this.deceasedId = deceasedId;
    }

    @Override
    public String toString() {
        return "Liability{" +
                "liabilityId=" + liabilityId +
                ", details='" + details + '\'' +
                ", amount=" + amount +
                ", deceasedId=" + deceasedId +
                '}';
    }
}
