package com.wirasat.model;

public class Beneficiary {
    private int beneficiaryId;
    private String beneficiaryName;
    private String beneficiaryType;
    private Integer memberId;

    public Beneficiary() {
    }

    public Beneficiary(int beneficiaryId, String beneficiaryName, String beneficiaryType, Integer memberId) {
        this.beneficiaryId = beneficiaryId;
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryType = beneficiaryType;
        this.memberId = memberId;
    }

    public int getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(int beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getBeneficiaryType() {
        return beneficiaryType;
    }

    public void setBeneficiaryType(String beneficiaryType) {
        this.beneficiaryType = beneficiaryType;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    @Override
    public String toString() {
        return "Beneficiary{" +
                "beneficiaryId=" + beneficiaryId +
                ", beneficiaryName='" + beneficiaryName + '\'' +
                ", beneficiaryType='" + beneficiaryType + '\'' +
                ", memberId=" + memberId +
                '}';
    }
}
