package com.wirasat.model;

import java.math.BigDecimal;

public class DistributionLog {
    private int logId;
    private BigDecimal shareFraction;
    private BigDecimal shareAmount;
    private int deceasedId;
    private int heirId;
    private int runId;

    public DistributionLog() {
    }

    public DistributionLog(int logId, BigDecimal shareFraction, BigDecimal shareAmount, int deceasedId, int heirId, int runId) {
        this.logId = logId;
        this.shareFraction = shareFraction;
        this.shareAmount = shareAmount;
        this.deceasedId = deceasedId;
        this.heirId = heirId;
        this.runId = runId;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public BigDecimal getShareFraction() {
        return shareFraction;
    }

    public void setShareFraction(BigDecimal shareFraction) {
        this.shareFraction = shareFraction;
    }

    public BigDecimal getShareAmount() {
        return shareAmount;
    }

    public void setShareAmount(BigDecimal shareAmount) {
        this.shareAmount = shareAmount;
    }

    public int getDeceasedId() {
        return deceasedId;
    }

    public void setDeceasedId(int deceasedId) {
        this.deceasedId = deceasedId;
    }

    public int getHeirId() {
        return heirId;
    }

    public void setHeirId(int heirId) {
        this.heirId = heirId;
    }

    public int getRunId() {
        return runId;
    }

    public void setRunId(int runId) {
        this.runId = runId;
    }

    @Override
    public String toString() {
        return "DistributionLog{" +
                "logId=" + logId +
                ", shareFraction=" + shareFraction +
                ", shareAmount=" + shareAmount +
                ", deceasedId=" + deceasedId +
                ", heirId=" + heirId +
                ", runId=" + runId +
                '}';
    }
}
