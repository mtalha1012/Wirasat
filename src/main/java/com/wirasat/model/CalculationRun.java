package com.wirasat.model;

import java.math.BigDecimal;
import java.util.Date;

public class CalculationRun {
    private int runId;
    private int deceasedId;
    private Date runDate;
    private BigDecimal netEstate;

    public CalculationRun() {
    }

    public CalculationRun(int runId, int deceasedId, Date runDate, BigDecimal netEstate) {
        this.runId = runId;
        this.deceasedId = deceasedId;
        this.runDate = runDate;
        this.netEstate = netEstate;
    }

    public int getRunId() {
        return runId;
    }

    public void setRunId(int runId) {
        this.runId = runId;
    }

    public int getDeceasedId() {
        return deceasedId;
    }

    public void setDeceasedId(int deceasedId) {
        this.deceasedId = deceasedId;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public BigDecimal getNetEstate() {
        return netEstate;
    }

    public void setNetEstate(BigDecimal netEstate) {
        this.netEstate = netEstate;
    }

    private java.util.List<AssetAllocation> allocations = new java.util.ArrayList<>();

    public java.util.List<AssetAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(java.util.List<AssetAllocation> allocations) {
        this.allocations = allocations;
    }

    @Override
    public String toString() {
        return "CalculationRun{" +
                "runId=" + runId +
                ", deceasedId=" + deceasedId +
                ", runDate=" + runDate +
                ", netEstate=" + netEstate +
                '}';
    }
}
