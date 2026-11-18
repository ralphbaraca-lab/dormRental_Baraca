
package com.dormrental.model;

import com.dormrental.enums.Amenity;
import com.dormrental.enums.RoomStatus;
import com.dormrental.enums.RoomType;
import com.dormrental.exception.BusinessRuleException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a room inside a dormitory.
 * The monthly rental rate is charged per bed.
 * A double room with two occupants collects two monthly rates.
 */
public class Room implements Identifiable, Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String dormitoryId;
    private final String roomNumber;
    private final int floor;
    private final RoomType type;

    private BigDecimal monthlyRate;

    private final EnumSet<Amenity> amenities =
            EnumSet.noneOf(Amenity.class);

    private boolean underMaintenance;

    private final ArrayList<String> occupantIds = new ArrayList<>();

    // Constructor
    public Room(String id, String dormitoryId, String roomNumber,
                int floor, RoomType type, BigDecimal monthlyRate) {

        this.id = validateText(id, "Room ID");
        this.dormitoryId = validateText(dormitoryId, "Dormitory ID");
        this.roomNumber = validateText(roomNumber, "Room number");

        if (floor < 0) {
            throw new IllegalArgumentException(
                "Floor cannot be negative."
            );
        }

        this.floor = floor;
        this.type = Objects.requireNonNull(
            type, "Room type cannot be null."
        );

        setMonthlyRate(monthlyRate);
    }

    // Validates required text fields
    private static String validateText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null.");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " cannot be empty."
            );
        }

        return value.trim();
    }

    // Returns room ID
    @Override
    public String getId() {
        return id;
    }

    // Returns parent dormitory ID
    public String getDormitoryId() {
        return dormitoryId;
    }

    // Returns room number
    public String getRoomNumber() {
        return roomNumber;
    }

    // Returns floor number
    public int getFloor() {
        return floor;
    }

    // Returns room type
    public RoomType getType() {
        return type;
    }

    // Returns monthly rental rate per bed
    public BigDecimal getMonthlyRate() {
        return monthlyRate;
    }

    // Updates monthly rental rate
    public void setMonthlyRate(BigDecimal monthlyRate) {
        Objects.requireNonNull(
            monthlyRate, "Monthly rate cannot be null."
        );

        if (monthlyRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "Monthly rate cannot be negative."
            );
        }

        this.monthlyRate = monthlyRate;
    }

    // Returns room amenities without allowing external modification
    public Set<Amenity> getAmenities() {
        return Collections.unmodifiableSet(amenities);
    }

    // Replaces room amenities
    public void setAmenities(Set<Amenity> newAmenities) {
        Objects.requireNonNull(
            newAmenities, "Amenities cannot be null."
        );

        if (newAmenities.contains(null)) {
            throw new IllegalArgumentException(
                "Amenities cannot contain null values."
            );
        }

        amenities.clear();
        amenities.addAll(newAmenities);
    }

    // Checks maintenance status
    public boolean isUnderMaintenance() {
        return underMaintenance;
    }

    // Updates maintenance status
    public void setUnderMaintenance(boolean underMaintenance) {
        this.underMaintenance = underMaintenance;
    }

    // Returns occupant IDs without allowing external modification
    public List<String> getOccupantIds() {
        return Collections.unmodifiableList(occupantIds);
    }

    // Returns maximum room capacity
    public int getCapacity() {
        return type.getCapacity();
    }

    // Returns current number of occupants
    public int getOccupancy() {
        return occupantIds.size();
    }

    // Returns remaining available beds
    public int getAvailableBeds() {
        return getCapacity() - getOccupancy();
    }

    // Checks whether the room is full
    public boolean isFull() {
        return getOccupancy() >= getCapacity();
    }

    // Checks whether a tenant can move into the room
    public boolean isRentable() {
        return !underMaintenance && !isFull();
    }

    // Returns the current room status
    public RoomStatus getStatus() {

        if (underMaintenance) {
            return RoomStatus.UNDER_MAINTENANCE;
        }

        if (getOccupancy() == 0) {
            return RoomStatus.VACANT;
        }

        return isFull()
                ? RoomStatus.FULL
                : RoomStatus.PARTIAL;
    }

    // Checks whether a specific tenant occupies this room
    public boolean hasOccupant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }

        return occupantIds.contains(tenantId.trim());
    }

    /**
     * Adds a tenant to the room.
     * Prevents duplicate occupants, overcapacity,
     * and occupancy during maintenance.
     */
    public void addOccupant(String tenantId) {

        tenantId = validateText(tenantId, "Tenant ID");

        if (hasOccupant(tenantId)) {
            throw new BusinessRuleException(
                "Tenant " + tenantId +
                " is already assigned to room " + roomNumber + "."
            );
        }

        if (underMaintenance) {
            throw new BusinessRuleException(
                "Room " + roomNumber + " is under maintenance."
            );
        }

        if (isFull()) {
            throw new BusinessRuleException(
                "Room " + roomNumber + " has reached its maximum capacity."
            );
        }

        occupantIds.add(tenantId);
    }

    /**
     * Removes a tenant from the room.
     * Returns true if the tenant was successfully removed.
     */
    public boolean removeOccupant(String tenantId) {

        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }

        return occupantIds.remove(tenantId.trim());
    }

    // Returns readable room information
    @Override
    public String toString() {
        return "Room {" +
                "ID='" + id + '\'' +
                ", Dormitory ID='" + dormitoryId + '\'' +
                ", Room Number='" + roomNumber + '\'' +
                ", Floor=" + floor +
                ", Type=" + type +
                ", Monthly Rate=" + monthlyRate +
                ", Occupancy=" + getOccupancy() + "/" + getCapacity() +
                ", Available Beds=" + getAvailableBeds() +
                ", Status=" + getStatus() +
                ", Amenities=" + amenities +
                '}';
    }
}
