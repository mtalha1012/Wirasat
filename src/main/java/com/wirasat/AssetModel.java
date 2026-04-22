package com.wirasat;

public class AssetModel {
    private String title;
    private String category;
    private String value;
    private String owner;
    private boolean shareable;
    private String colorCode;

    public AssetModel(String title, String category, String value, String owner, boolean shareable, String colorCode) {
        this.title = title;
        this.category = category;
        this.value = value;
        this.owner = owner;
        this.shareable = shareable;
        this.colorCode = colorCode;
    }

    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getValue() { return value; }
    public String getOwner() { return owner; }
    public boolean isShareable() { return shareable; }
    public String getColorCode() { return colorCode; }
}