package com.dormrental.model;

import com.dormrental.enums.DormType;

import java.io.Serializable;

/** A dormitory building. Rooms point back to it through their dormitoryId. */
public class Dormitory implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private String address;
    private DormType type;
    private String contactNumber;

    public Dormitory(String id, String name, String address, DormType type, String contactNumber) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.type = type;
        this.contactNumber = contactNumber;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public DormType getType() {
        return type;
    }

    public void setType(DormType type) {
        this.type = type;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
}
