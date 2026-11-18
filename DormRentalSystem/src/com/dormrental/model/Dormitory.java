
package com.dormrental.model;

import com.dormrental.enums.DormType;
import java.io.Serializable;

/**
 * Represents a dormitory building.
 * Each dormitory can contain multiple rooms.
 */
public class Dormitory implements Identifiable, Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private String address;
    private DormType type;
    private String contactNumber;

    // Constructor
    public Dormitory(String id, String name, String address,
                     DormType type, String contactNumber) {

        this.id = requireText(id, "Dormitory ID");
        setName(name);
        setAddress(address);
        setType(type);
        setContactNumber(contactNumber);
    }

    // Validates required text fields
    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " cannot be empty."
            );
        }

        return value.trim();
    }

    // Returns the dormitory ID
    @Override
    public String getId() {
        return id;
    }

    // Name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "Dormitory name");
    }

    // Address
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = requireText(address, "Dormitory address");
    }

    // Dormitory type
    public DormType getType() {
        return type;
    }

    public void setType(DormType type) {
        if (type == null) {
            throw new IllegalArgumentException(
                "Dormitory type cannot be null."
            );
        }

        this.type = type;
    }

    // Contact number
    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = requireText(
            contactNumber, "Contact number"
        );
    }

    // Returns a readable dormitory description
    @Override
    public String toString() {
        return "Dormitory {" +
                "ID='" + id + '\'' +
                ", Name='" + name + '\'' +
                ", Address='" + address + '\'' +
                ", Type='" + type + '\'' +
                ", Contact='" + contactNumber + '\'' +
                '}';
    }
}
