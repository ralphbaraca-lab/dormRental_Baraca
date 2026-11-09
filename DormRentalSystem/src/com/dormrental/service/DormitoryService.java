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
                contactNumber == null ? "" : contactNumber.trim());
        return dormitories.save(dormitory);
    }

    /** Blank / null arguments mean "keep the current value". */
    public Dormitory updateDormitory(String id, String name, String address, DormType type, String contactNumber) {
        Dormitory dormitory = getDormitory(id);
        if (!Validator.isBlank(name)) {
            requireUniqueName(name.trim(), id);
            dormitory.setName(name.trim());
        }
        if (!Validator.isBlank(address)) {
            dormitory.setAddress(address.trim());
        }
        if (!Validator.isBlank(contactNumber)) {
            dormitory.setContactNumber(contactNumber.trim());
        }
        if (type != null && type != dormitory.getType()) {
            boolean hasOccupants = !rooms.findAll(r -> r.getDormitoryId().equals(id) && r.getOccupancy() > 0).isEmpty();
            if (hasOccupants) {
                throw new BusinessRuleException("Cannot change the dormitory type while tenants are living in it.");
            }
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
        if (!rooms.findAll(r -> r.getDormitoryId().equals(id)).isEmpty()) {
            throw new BusinessRuleException("Dormitory '" + dormitory.getName() + "' still has rooms. Delete the rooms first.");
        }
        dormitories.deleteById(id);
    }

    private void requireUniqueName(String name, String ignoreId) {
        boolean taken = !dormitories.findAll(d -> d.getName().equalsIgnoreCase(name) && !d.getId().equals(ignoreId)).isEmpty();
        if (taken) {
            throw new DuplicateEntityException("A dormitory named '" + name + "' already exists.");
        }
    }
}
