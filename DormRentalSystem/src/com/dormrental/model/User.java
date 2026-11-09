package com.dormrental.model;

import com.dormrental.enums.UserRole;

import java.io.Serializable;

/** A staff account that can log in to the system. */
public class User implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String username;
    private String passwordHash;
    private String fullName;
    private final UserRole role;

    public User(String id, String username, String passwordHash, String fullName, UserRole role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
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
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserRole getRole() {
        return role;
    }
}
