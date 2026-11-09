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
import java.util.Set;

/**
 * A room inside a dormitory. The monthly rate is charged per bed, so a double room with
 * two tenants collects two rates.
 */
public class Room implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String dormitoryId;
    private final String roomNumber;
    private final int floor;
    private final RoomType type;
    private BigDecimal monthlyRate;
    private final EnumSet<Amenity> amenities = EnumSet.noneOf(Amenity.class);
    private boolean underMaintenance;
    private final ArrayList<String> occupantIds = new ArrayList<>();

    public Room(String id, String dormitoryId, String roomNumber, int floor, RoomType type, BigDecimal monthlyRate) {
        this.id = id;
        this.dormitoryId = dormitoryId;
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.type = type;
        this.monthlyRate = monthlyRate;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getDormitoryId() {
        return dormitoryId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public int getFloor() {
        return floor;
    }

    public RoomType getType() {
        return type;
    }

    public BigDecimal getMonthlyRate() {
        return monthlyRate;
    }

    public void setMonthlyRate(BigDecimal monthlyRate) {
        this.monthlyRate = monthlyRate;
    }

    public Set<Amenity> getAmenities() {
        return Collections.unmodifiableSet(amenities);
    }

    public void setAmenities(Set<Amenity> newAmenities) {
        amenities.clear();
        amenities.addAll(newAmenities);
    }

    public boolean isUnderMaintenance() {
        return underMaintenance;
    }

    public void setUnderMaintenance(boolean underMaintenance) {
        this.underMaintenance = underMaintenance;
    }

    public List<String> getOccupantIds() {
        return Collections.unmodifiableList(occupantIds);
    }

    public int getCapacity() {
        return type.getCapacity();
    }

    public int getOccupancy() {
        return occupantIds.size();
    }

    public int getAvailableBeds() {
        return getCapacity() - getOccupancy();
    }

    public boolean isFull() {
        return getOccupancy() >= getCapacity();
    }

    /** True if a new tenant could move in right now. */
    public boolean isRentable() {
        return !underMaintenance && !isFull();
    }

    public RoomStatus getStatus() {
        if (underMaintenance) {
            return RoomStatus.UNDER_MAINTENANCE;
        }
        if (getOccupancy() == 0) {
            return RoomStatus.VACANT;
        }
        return isFull() ? RoomStatus.FULL : RoomStatus.PARTIAL;
    }

    public void addOccupant(String tenantId) {
        if (occupantIds.contains(tenantId)) {
            throw new BusinessRuleException("Tenant " + tenantId + " is already in room " + roomNumber + ".");
        }
        if (isFull()) {
            throw new BusinessRuleException("Room " + roomNumber + " is already full.");
        }
        occupantIds.add(tenantId);
    }

    public boolean removeOccupant(String tenantId) {
        return occupantIds.remove(tenantId);
    }
}
