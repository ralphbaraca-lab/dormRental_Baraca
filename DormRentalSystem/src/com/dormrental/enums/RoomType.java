
package com.dormrental.enums;

/**
 * Represents the available room layouts in the dormitory.
 * Each room type defines its display label and maximum bed capacity.
 */
public enum RoomType {

    SINGLE("Single", 1),
    DOUBLE("Double", 2),
    TRIPLE("Triple", 3),
    QUAD("Quad", 4);

    private final String label;
    private final int capacity;

    // Constructor
    RoomType(String label, int capacity) {
        this.label = label;
        this.capacity = capacity;
    }

    // Returns the room type label
    public String getLabel() {
        return label;
    }

    // Returns the maximum number of beds
    public int getCapacity() {
        return capacity;
    }

    // Returns the room type with its bed capacity
    @Override
    public String toString() {
        return String.format(
            "%s (%d %s)",
            label,
            capacity,
            capacity == 1 ? "bed" : "beds"
        );
    }

    // Finds a room type using its label
    public static RoomType fromLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Room type label cannot be empty.");
        }

        for (RoomType type : values()) {
            if (type.label.equalsIgnoreCase(label.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException(
            "Unknown room type: " + label
        );
    }
}
