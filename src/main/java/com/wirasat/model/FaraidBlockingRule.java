package com.wirasat.model;

public class FaraidBlockingRule {
    private int ruleId;
    private int targetRelationId;
    private int blockingRelationId;

    public FaraidBlockingRule() {
    }

    public FaraidBlockingRule(int ruleId, int targetRelationId, int blockingRelationId) {
        this.ruleId = ruleId;
        this.targetRelationId = targetRelationId;
        this.blockingRelationId = blockingRelationId;
    }

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public int getTargetRelationId() {
        return targetRelationId;
    }

    public void setTargetRelationId(int targetRelationId) {
        this.targetRelationId = targetRelationId;
    }

    public int getBlockingRelationId() {
        return blockingRelationId;
    }

    public void setBlockingRelationId(int blockingRelationId) {
        this.blockingRelationId = blockingRelationId;
    }

    @Override
    public String toString() {
        return "FaraidBlockingRule{" +
                "ruleId=" + ruleId +
                ", targetRelationId=" + targetRelationId +
                ", blockingRelationId=" + blockingRelationId +
                '}';
    }
}
