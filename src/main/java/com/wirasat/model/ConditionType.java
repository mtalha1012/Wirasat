package com.wirasat.model;

public class ConditionType {
    private int conditionId;
    private String conditionName;
    private String description;

    public ConditionType() {
    }

    public ConditionType(int conditionId, String conditionName, String description) {
        this.conditionId = conditionId;
        this.conditionName = conditionName;
        this.description = description;
    }

    public int getConditionId() {
        return conditionId;
    }

    public void setConditionId(int conditionId) {
        this.conditionId = conditionId;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ConditionType{" +
                "conditionId=" + conditionId +
                ", conditionName='" + conditionName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
