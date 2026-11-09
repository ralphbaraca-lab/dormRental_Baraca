package com.dormrental.enums;

/** Who is allowed to live in a dormitory building. */
public enum DormType {
    MALE_ONLY("Male only"),
    FEMALE_ONLY("Female only"),
    CO_ED("Co-ed");

    private final String label;

    DormType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Returns true if a tenant of the given gender may live in this type of dormitory. */
    public boolean accepts(Gender gender) {
        switch (this) {
            case MALE_ONLY:
                return gender == Gender.MALE;
            case FEMALE_ONLY:
                return gender == Gender.FEMALE;
            default:
                return true;
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
