package com.dormrental.model;

import com.dormrental.enums.Gender;
import java.time.LocalDate;
import java.util.Objects;

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
        this.gender = Objects.requireNonNull(gender, "gender must not be null");
        this.registeredOn = LocalDate.now();
        setSchool(school);
        setHomeAddress(homeAddress);
        setGuardianName(guardianName);
        setGuardianPhone(guardianPhone);
    }

    public Gender getGender() {
        return gender;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = normalizeOptional(school);
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = normalizeOptional(homeAddress);
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = normalizeOptional(guardianName);
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    public void setGuardianPhone(String guardianPhone) {
        this.guardianPhone = normalizeOptional(guardianPhone);
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

    @Override
    public String toString() {
        return "Tenant{" +
                "gender=" + gender +
                ", school='" + school + '\'' +
                ", homeAddress='" + homeAddress + '\'' +
                ", guardianName='" + guardianName + '\'' +
                ", guardianPhone='" + guardianPhone + '\'' +
                ", registeredOn=" + registeredOn +
                ", active=" + active +
                '}';
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}