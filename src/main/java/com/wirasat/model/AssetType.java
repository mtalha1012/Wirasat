package com.wirasat.model;

public class AssetType {
    private int typeId;
    private String categoryName;

    public AssetType() {
    }

    public AssetType(int typeId, String categoryName) {
        this.typeId = typeId;
        this.categoryName = categoryName;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    @Override
    public String toString() {
        return "AssetType{" +
                "typeId=" + typeId +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }
}
