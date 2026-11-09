package com.dormrental.enums;

/** Room layouts. The capacity is the number of beds in the room. */
public enum RoomType {
    SINGLE("Single", 1),
    DOUBLE("Double", 2),
    TRIPLE("Triple", 3),
    QUAD("Quad", 4);

    private final String label;
    private final int capacity;

    RoomType(String label, int capacity) {
        this.label = label;
        this.capacity = capacity;
    }

    public String getLabel() {
        return label;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return label + " (" + capacity + (capacity == 1 ? " bed)" : " beds)");
    }
}
