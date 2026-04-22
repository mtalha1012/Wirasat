package com.wirasat.model;

import java.math.BigDecimal;

public class Wasiyat {
    private int willId;
    private int deceasedId;
    private int beneficiaryId;
    private BigDecimal amount;

    public Wasiyat() {
    }

    public Wasiyat(int willId, int deceasedId, int beneficiaryId, BigDecimal amount) {
        this.willId = willId;
        this.deceasedId = deceasedId;
        this.beneficiaryId = beneficiaryId;
        this.amount = amount;
    }

    public int getWillId() {
        return willId;
    }

    public void setWillId(int willId) {
        this.willId = willId;
    }

    public int getDeceasedId() {
        return deceasedId;
    }

    public void setDeceasedId(int deceasedId) {
        this.deceasedId = deceasedId;
    }

    public int getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(int beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Wasiyat{" +
                "willId=" + willId +
                ", deceasedId=" + deceasedId +
                ", beneficiaryId=" + beneficiaryId +
                ", amount=" + amount +
                '}';
    }
}
