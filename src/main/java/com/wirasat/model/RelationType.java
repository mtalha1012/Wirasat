package com.wirasat.model;

public class RelationType {
    private int relationId;
    private String relationName;
    private String category;

    public RelationType() {
    }

    public RelationType(int relationId, String relationName, String category) {
        this.relationId = relationId;
        this.relationName = relationName;
        this.category = category;
    }

    public int getRelationId() {
        return relationId;
    }

    public void setRelationId(int relationId) {
        this.relationId = relationId;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "RelationType{" +
                "relationId=" + relationId +
                ", relationName='" + relationName + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}
