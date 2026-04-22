package com.wirasat.model;

public class DeceasedHeir {
    private int mappingId;
    private int deceasedId;
    private int heirId;
    private int relationId;

    public DeceasedHeir() {
    }

    public DeceasedHeir(int mappingId, int deceasedId, int heirId, int relationId) {
        this.mappingId = mappingId;
        this.deceasedId = deceasedId;
        this.heirId = heirId;
        this.relationId = relationId;
    }

    public int getMappingId() {
        return mappingId;
    }

    public void setMappingId(int mappingId) {
        this.mappingId = mappingId;
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

    public int getRelationId() {
        return relationId;
    }

    public void setRelationId(int relationId) {
        this.relationId = relationId;
    }

    @Override
    public String toString() {
        return "DeceasedHeir{" +
                "mappingId=" + mappingId +
                ", deceasedId=" + deceasedId +
                ", heirId=" + heirId +
                ", relationId=" + relationId +
                '}';
    }
}
