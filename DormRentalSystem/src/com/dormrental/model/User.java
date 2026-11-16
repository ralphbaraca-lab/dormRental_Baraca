package com.dormrental.model;

import com.dormrental.enums.UserRole;

import java.io.Serializable;
import java.util.Objects;

/** A staff account that can log in to the system. */
public class User implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String username;
    private String passwordHash;
    private String fullName;
    private final UserRole role;

    public User(String id, String username, String passwordHash, String fullName, UserRole role) {
        this.id = requireNonBlank(id, "id");
        this.username = requireNonBlank(username, "username");
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
        this.fullName = requireNonBlank(fullName, "fullName");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    @Override
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = requireNonBlank(fullName, "fullName");
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                '}';
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
        return value.trim();
    }
}