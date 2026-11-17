package com.dormrental.service;

import com.dormrental.enums.DormType;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.exception.DuplicateEntityException;
import com.dormrental.exception.EntityNotFoundException;
import com.dormrental.model.Dormitory;
import com.dormrental.model.Room;
import com.dormrental.repository.Repository;
import com.dormrental.util.Validator;
import java.util.List;

public class DormitoryService {
    private final Repository<Dormitory> dormitories;
    private final Repository<Room> rooms;

    public DormitoryService(Repository<Dormitory> dormitories, Repository<Room> rooms) {
        this.dormitories = dormitories;
        this.rooms = rooms;
    }

    public Dormitory addDormitory(String name, String address, DormType type, String contactNumber) {
        String cleanName = Validator.requireNotBlank(name, "Dormitory name");
        String cleanAddress = Validator.requireNotBlank(address, "Address");
        if (type == null) {
            throw new BusinessRuleException("Dormitory type is required.");
        }
        requireUniqueName(cleanName, null);
        Dormitory dormitory = new Dormitory(dormitories.nextId(), cleanName, cleanAddress, type,
                trimOrEmpty(contactNumber));
        return dormitories.save(dormitory);
    }

    /** Blank / null arguments mean "keep the current value". */
    public Dormitory updateDormitory(String id, String name, String address, DormType type, String contactNumber) {
        Dormitory dormitory = getDormitory(id);

        // Check everything first and change the dormitory only afterwards, so a rejected update
        // can't leave a half-applied change on the object.
        String newName = Validator.isBlank(name) ? null : name.trim();
        if (newName != null) {
            requireUniqueName(newName, id);
        }
        boolean changesType = type != null && type != dormitory.getType();
        if (changesType && hasOccupants(id)) {
            throw new BusinessRuleException("Cannot change the dormitory type while tenants are living in it.");
        }

        if (newName != null) {
            dormitory.setName(newName);
        }
        if (!Validator.isBlank(address)) {
            dormitory.setAddress(address.trim());
        }
        if (!Validator.isBlank(contactNumber)) {
            dormitory.setContactNumber(contactNumber.trim());
        }
        if (changesType) {
            dormitory.setType(type);
        }
        return dormitories.save(dormitory);
    }

    public Dormitory getDormitory(String id) {
        return dormitories.findById(id).orElseThrow(() -> new EntityNotFoundException("Dormitory", id));
    }

    public List<Dormitory> listDormitories() {
        return dormitories.findAll();
    }

    public void deleteDormitory(String id) {
        Dormitory dormitory = getDormitory(id);
        if (hasRooms(id)) {
            throw new BusinessRuleException("Dormitory '" + dormitory.getName() + "' still has rooms. Delete the rooms first.");
        }
        dormitories.deleteById(id);
    }

    private boolean hasRooms(String dormitoryId) {
        return !rooms.findAll(r -> r.getDormitoryId().equals(dormitoryId)).isEmpty();
    }

    private boolean hasOccupants(String dormitoryId) {
        return !rooms.findAll(r -> r.getDormitoryId().equals(dormitoryId) && r.getOccupancy() > 0).isEmpty();
    }

    private void requireUniqueName(String name, String ignoreId) {
        boolean taken = !dormitories.findAll(d -> d.getName().equalsIgnoreCase(name) && !d.getId().equals(ignoreId)).isEmpty();
        if (taken) {
            throw new DuplicateEntityException("A dormitory named '" + name + "' already exists.");
        }
    }

    private static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}