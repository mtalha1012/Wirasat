package com.wirasat.model;

import java.util.Date;

public class FamilyMember {
    private int memberId;
    private String cnic;
    private String name;
    private Date dateOfBirth;
    private char gender;
    private int age;
    private Date dateOfDeath;
    private Integer fatherId;
    private Integer motherId;

    public FamilyMember() {
    }

    public FamilyMember(int memberId, String cnic, String name, Date dateOfBirth, char gender, int age, Date dateOfDeath, Integer fatherId, Integer motherId) {
        this.memberId = memberId;
        this.cnic = cnic;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.age = age;
        this.dateOfDeath = dateOfDeath;
        this.fatherId = fatherId;
        this.motherId = motherId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public String getCnic() {
        return cnic;
    }

    public void setCnic(String cnic) {
        this.cnic = cnic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public char getGender() {
        return gender;
    }

    public void setGender(char gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Date getDateOfDeath() {
        return dateOfDeath;
    }

    public void setDateOfDeath(Date dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    public Integer getFatherId() {
        return fatherId;
    }

    public void setFatherId(Integer fatherId) {
        this.fatherId = fatherId;
    }

    public Integer getMotherId() {
        return motherId;
    }

    public void setMotherId(Integer motherId) {
        this.motherId = motherId;
    }

    @Override
    public String toString() {
        return "FamilyMember{" +
                "memberId=" + memberId +
                ", cnic='" + cnic + '\'' +
                ", name='" + name + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", gender=" + gender +
                ", age=" + age +
                ", dateOfDeath=" + dateOfDeath +
                ", fatherId=" + fatherId +
                ", motherId=" + motherId +
                '}';
    }
}
