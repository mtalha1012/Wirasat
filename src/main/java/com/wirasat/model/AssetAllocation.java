package com.wirasat.model;

import java.math.BigDecimal;

public class AssetAllocation {
    private int allocationId;
    private int assetId;
    private int heirId;
    private BigDecimal allocatedPercentage;
    private BigDecimal allocatedValue;
    private boolean isFinalized;

    public AssetAllocation() {
    }

    public AssetAllocation(int allocationId, int assetId, int heirId, BigDecimal allocatedPercentage, BigDecimal allocatedValue, boolean isFinalized) {
        this.allocationId = allocationId;
        this.assetId = assetId;
        this.heirId = heirId;
        this.allocatedPercentage = allocatedPercentage;
        this.allocatedValue = allocatedValue;
        this.isFinalized = isFinalized;
    }

    public int getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(int allocationId) {
        this.allocationId = allocationId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public int getHeirId() {
        return heirId;
    }

    public void setHeirId(int heirId) {
        this.heirId = heirId;
    }

    public BigDecimal getAllocatedPercentage() {
        return allocatedPercentage;
    }

    public void setAllocatedPercentage(BigDecimal allocatedPercentage) {
        this.allocatedPercentage = allocatedPercentage;
    }

    public BigDecimal getAllocatedValue() {
        return allocatedValue;
    }

    public void setAllocatedValue(BigDecimal allocatedValue) {
        this.allocatedValue = allocatedValue;
    }

    public boolean isFinalized() {
        return isFinalized;
    }

    public void setFinalized(boolean finalized) {
        isFinalized = finalized;
    }

    @Override
    public String toString() {
        return "AssetAllocation{" +
                "allocationId=" + allocationId +
                ", assetId=" + assetId +
                ", heirId=" + heirId +
                ", allocatedPercentage=" + allocatedPercentage +
                ", allocatedValue=" + allocatedValue +
                ", isFinalized=" + isFinalized +
                '}';
    }
}
