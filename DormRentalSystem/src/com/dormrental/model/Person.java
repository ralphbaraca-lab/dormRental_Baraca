package com.dormrental.model;

import java.io.Serializable;
import java.util.Objects;

/** Shared contact details for any person in the system. */
public abstract class Person implements Identifiable, Serializable {
    // Keep this value: changing it (or the fields below) makes records already saved to disk unreadable.
    private static final long serialVersionUID = 1L;

    private final String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    protected Person(String id, String firstName, String lastName, String email, String phone) {
        // Assigned directly instead of through the setters, because a subclass could override those
        // and its version would run before the subclass has been set up.
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.phone = Objects.requireNonNull(phone, "phone must not be null");
    }

    @Override
    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = requireText(firstName, "firstName");
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = requireText(lastName, "lastName");
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "email must not be null");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = Objects.requireNonNull(phone, "phone must not be null");
    }

    /** Two people are the same record if they are the same kind of person with the same id. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id.equals(((Person) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }
}