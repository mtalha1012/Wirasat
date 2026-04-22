package com.wirasat.model;

public class ShareRule {
    private int ruleId;
    private int relationId;
    private int numerator;
    private int denominator;
    private String conditionType; // e.g., "DEFAULT", "WITH_CHILD", "NO_CHILD"

    public ShareRule() {
    }

    public ShareRule(int ruleId, int relationId, int numerator, int denominator, String conditionType) {
        this.ruleId = ruleId;
        this.relationId = relationId;
        this.numerator = numerator;
        this.denominator = denominator;
        this.conditionType = conditionType;
    }

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public int getRelationId() {
        return relationId;
    }

    public void setRelationId(int relationId) {
        this.relationId = relationId;
    }

    public int getNumerator() {
        return numerator;
    }

    public void setNumerator(int numerator) {
        this.numerator = numerator;
    }

    public int getDenominator() {
        return denominator;
    }

    public void setDenominator(int denominator) {
        this.denominator = denominator;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    @Override
    public String toString() {
        return "ShareRule{" +
                "ruleId=" + ruleId +
                ", relationId=" + relationId +
                ", fraction=" + numerator + "/" + denominator +
                ", conditionType='" + conditionType + '\'' +
                '}';
    }
}
