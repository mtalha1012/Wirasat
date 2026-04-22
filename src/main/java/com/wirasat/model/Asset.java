package com.wirasat.model;

public class Asset {
    private int assetId;
    private String assetName;
    private int typeId;
    private int ownerId;
    private boolean isShareable;
    private java.math.BigDecimal value;

    public Asset() {
    }

    public Asset(int assetId, String assetName, int typeId, int ownerId, boolean isShareable) {
        this.assetId = assetId;
        this.assetName = assetName;
        this.typeId = typeId;
        this.ownerId = ownerId;
        this.isShareable = isShareable;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public java.math.BigDecimal getValue() { return java.math.BigDecimal.ZERO; }
    public boolean isShareable() {
        return isShareable;
    }

    public void setShareable(boolean shareable) {
        isShareable = shareable;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "assetId=" + assetId +
                ", assetName='" + assetName + '\'' +
                ", typeId=" + typeId +
                ", ownerId=" + ownerId +
                ", isShareable=" + isShareable +
                '}';
    }
}
