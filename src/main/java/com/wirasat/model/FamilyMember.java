package com.wirasat.model;

import java.util.Calendar;
import java.util.Date;

/**
 * Mirrors schema.sql family_members table.
 * age is a VIRTUAL computed column in MySQL — we replicate it here
 * as TIMESTAMPDIFF(YEAR, date_of_birth, IFNULL(date_of_death, CURDATE())).
 */
public class FamilyMember {
    private int memberId;
    private String cnic;
    private String name;
    private Date dateOfBirth;
    private char gender;
    private Date dateOfDeath;
    private Integer fatherId;
    private Integer motherId;

    public FamilyMember() {
    }

    /** Constructor matching schema columns (age excluded — it is virtual/computed). */
    public FamilyMember(int memberId, String cnic, String name, Date dateOfBirth, char gender, Date dateOfDeath, Integer fatherId, Integer motherId) {
        this.memberId = memberId;
        this.cnic = cnic;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.dateOfDeath = dateOfDeath;
        this.fatherId = fatherId;
        this.motherId = motherId;
    }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getCnic() { return cnic; }
    public void setCnic(String cnic) { this.cnic = cnic; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public char getGender() { return gender; }
    public void setGender(char gender) { this.gender = gender; }

    /** Computed virtual column — mirrors schema: TIMESTAMPDIFF(YEAR, dob, IFNULL(dod, CURDATE())) */
    public int getAge() {
        if (dateOfBirth == null) return 0;
        Calendar ref = Calendar.getInstance();
        if (dateOfDeath != null) ref.setTime(dateOfDeath);
        Calendar dob = Calendar.getInstance();
        dob.setTime(dateOfBirth);
        int age = ref.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (ref.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--;
        return Math.max(age, 0);
    }

    public Date getDateOfDeath() { return dateOfDeath; }
    public void setDateOfDeath(Date dateOfDeath) { this.dateOfDeath = dateOfDeath; }

    public Integer getFatherId() { return fatherId; }
    public void setFatherId(Integer fatherId) { this.fatherId = fatherId; }

    public Integer getMotherId() { return motherId; }
    public void setMotherId(Integer motherId) { this.motherId = motherId; }

    @Override
    public String toString() {
        return "FamilyMember{" +
                "memberId=" + memberId +
                ", cnic='" + cnic + '\'' +
                ", name='" + name + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", gender=" + gender +
                ", age=" + getAge() +
                ", dateOfDeath=" + dateOfDeath +
                ", fatherId=" + fatherId +
                ", motherId=" + motherId +
                '}';
    }
}
