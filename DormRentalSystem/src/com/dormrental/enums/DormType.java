
package com.dormrental.enums;

/**
 * Defines the permitted tenant gender for a dormitory.
 */
public enum DormType {

    MALE_ONLY("Male only", Gender.MALE),
    FEMALE_ONLY("Female only", Gender.FEMALE),
    CO_ED("Co-ed", null);

    private final String label;
    private final Gender permittedGender;

    // Constructor
    DormType(String label, Gender permittedGender) {
        this.label = label;
        this.permittedGender = permittedGender;
    }

    // Returns the display label
    public String getLabel() {
        return label;
    }

    /**
     * Checks whether a tenant's gender is permitted
     * in this dormitory type.
     */
    public boolean accepts(Gender gender) {
        if (gender == null) {
            return false;
        }

        return permittedGender == null || permittedGender == gender;
    }

    // Returns the dormitory type label
    @Override
    public String toString() {
        return label;
    }
}
