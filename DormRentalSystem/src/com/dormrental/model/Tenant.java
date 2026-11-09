package com.dormrental.model;

import com.dormrental.enums.Gender;

import java.time.LocalDate;

public class Tenant extends Person {
    private static final long serialVersionUID = 1L;

    private final Gender gender;
    private String school;
    private String homeAddress;
    private String guardianName;
    private String guardianPhone;
    private final LocalDate registeredOn;
    private boolean active = true;

    public Tenant(String id, String firstName, String lastName, String email, String phone,
                  Gender gender, String school, String homeAddress,
                  String guardianName, String guardianPhone) {
        super(id, firstName, lastName, email, phone);
        this.gender = gender;
        this.school = school;
        this.homeAddress = homeAddress;
        this.guardianName = guardianName;
        this.guardianPhone = guardianPhone;
        this.registeredOn = LocalDate.now();
    }

    public Gender getGender() {
        return gender;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    public void setGuardianPhone(String guardianPhone) {
        this.guardianPhone = guardianPhone;
    }

    public LocalDate getRegisteredOn() {
        return registeredOn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
