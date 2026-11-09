package com.dormrental.enums;

public enum Amenity {
    WIFI("Wi-Fi"),
    AIR_CONDITIONING("Air conditioning"),
    ELECTRIC_FAN("Electric fan"),
    PRIVATE_BATHROOM("Private bathroom"),
    WATER_HEATER("Water heater"),
    STUDY_DESK("Study desk"),
    CLOSET("Closet"),
    LAUNDRY_ACCESS("Laundry access");

    private final String label;

    Amenity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
