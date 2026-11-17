package com.dormrental.service;

import com.dormrental.enums.Amenity;
import com.dormrental.enums.RoomType;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.exception.DuplicateEntityException;
import com.dormrental.exception.EntityNotFoundException;
import com.dormrental.exception.ValidationException;
import com.dormrental.model.Room;
import com.dormrental.repository.Repository;
import com.dormrental.util.Validator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class RoomService {
    private final Repository<Room> rooms;
    private final DormitoryService dormitoryService;

    public RoomService(Repository<Room> rooms, DormitoryService dormitoryService) {
        this.rooms = rooms;
        this.dormitoryService = dormitoryService;
    }

    public Room addRoom(String dormitoryId, String roomNumber, int floor, RoomType type,
                        BigDecimal ratePerBed, Set<Amenity> amenities) {
        // Check the input itself first, before looking anything up.
        String number = Validator.requireNotBlank(roomNumber, "Room number");
        if (floor < 1) {
            throw new ValidationException("Floor must be 1 or higher.");
        }
        if (type == null) {
            throw new ValidationException("Room type is required.");
        }
        Validator.requirePositive(ratePerBed, "Monthly rate");

        dormitoryService.getDormitory(dormitoryId); // throws if it does not exist
        if (roomNumberTaken(dormitoryId, number)) {
            throw new DuplicateEntityException("Room " + number + " already exists in this dormitory.");
        }

        Room room = new Room(rooms.nextId(), dormitoryId, number, floor, type, ratePerBed);
        if (amenities != null) {
            room.setAmenities(amenities);
        }
        return rooms.save(room);
    }

    /** Null arguments mean "keep the current value". Existing leases keep the rate they were signed at. */
    public Room updateRoom(String roomId, BigDecimal newRate, Set<Amenity> newAmenities) {
        Room room = getRoom(roomId);
        if (newRate != null) {
            room.setMonthlyRate(Validator.requirePositive(newRate, "Monthly rate"));
        }
        if (newAmenities != null) {
            room.setAmenities(newAmenities);
        }
        return rooms.save(room);
    }

    public Room getRoom(String id) {
        return rooms.findById(id).orElseThrow(() -> new EntityNotFoundException("Room", id));
    }

    public List<Room> listRooms() {
        return rooms.findAll();
    }

    public List<Room> listByDormitory(String dormitoryId) {
        return rooms.findAll(r -> r.getDormitoryId().equals(dormitoryId));
    }

    /** Rooms where at least one bed is free and the room is not under maintenance. */
    public List<Room> listAvailable() {
        return rooms.findAll(Room::isRentable);
    }

    /** Any argument may be null, which means "don't filter on this". */
    public List<Room> searchAvailable(String dormitoryId, RoomType type, BigDecimal maxRate) {
        return rooms.findAll(r -> r.isRentable()
                && (dormitoryId == null || r.getDormitoryId().equals(dormitoryId))
                && (type == null || r.getType() == type)
                && (maxRate == null || r.getMonthlyRate().compareTo(maxRate) <= 0));
    }

    public Room setUnderMaintenance(String roomId, boolean underMaintenance) {
        Room room = getRoom(roomId);
        room.setUnderMaintenance(underMaintenance);
        return rooms.save(room);
    }

    public void deleteRoom(String roomId) {
        Room room = getRoom(roomId);
        if (room.getOccupancy() > 0) {
            throw new BusinessRuleException("Room " + room.getRoomNumber() + " still has tenants living in it.");
        }
        rooms.deleteById(roomId);
    }

    /** Used by LeaseService when a lease starts. */
    public Room assignOccupant(String roomId, String tenantId) {
        Room room = getRoom(roomId);
        if (room.isUnderMaintenance()) {
            throw new BusinessRuleException("Room " + room.getRoomNumber() + " is under maintenance.");
        }
        room.addOccupant(tenantId); // throws if the room is full
        return rooms.save(room);
    }

    /**
     * Used by LeaseService when a lease ends or is terminated. Does nothing if the room no longer
     * exists or the tenant isn't in it, so closing an old lease can never fail because of the room.
     */
    public void releaseOccupant(String roomId, String tenantId) {
        rooms.findById(roomId).ifPresent(room -> {
            if (room.removeOccupant(tenantId)) {
                rooms.save(room);
            }
        });
    }

    public int totalBeds() {
        return rooms.findAll().stream().mapToInt(Room::getCapacity).sum();
    }

    public int occupiedBeds() {
        return rooms.findAll().stream().mapToInt(Room::getOccupancy).sum();
    }

    private boolean roomNumberTaken(String dormitoryId, String roomNumber) {
        return !rooms.findAll(r -> r.getDormitoryId().equals(dormitoryId)
                && r.getRoomNumber().equalsIgnoreCase(roomNumber)).isEmpty();
    }
}