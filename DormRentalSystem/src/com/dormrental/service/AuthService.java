package com.dormrental.service;

import com.dormrental.enums.UserRole;
import com.dormrental.exception.AuthenticationException;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.exception.DuplicateEntityException;
import com.dormrental.exception.ValidationException;
import com.dormrental.model.User;
import com.dormrental.repository.Repository;
import com.dormrental.util.PasswordUtil;
import com.dormrental.util.Validator;

import java.util.List;
import java.util.Optional;

public class AuthService {
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final Repository<User> users;
    private User currentUser;

    public AuthService(Repository<User> users) {
        this.users = users;
        seedDefaultAdmin();
    }

    private void seedDefaultAdmin() {
        if (users.count() == 0) {
            users.save(new User(users.nextId(), DEFAULT_ADMIN_USERNAME,
                    PasswordUtil.hash(DEFAULT_ADMIN_PASSWORD), "System Administrator", UserRole.ADMIN));
        }
    }

    public User login(String username, String password) {
        String name = username == null ? "" : username.trim();
        Optional<User> found = users.findAll(u -> u.getUsername().equalsIgnoreCase(name)).stream().findFirst();
        if (!found.isPresent() || !PasswordUtil.verify(password == null ? "" : password, found.get().getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }
        currentUser = found.get();
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    public User registerUser(String username, String password, String fullName, UserRole role) {
        if (!isAdmin()) {
            throw new BusinessRuleException("Only administrators can create user accounts.");
        }
        String name = Validator.requireNotBlank(username, "Username");
        String full = Validator.requireNotBlank(fullName, "Full name");
        requireValidPassword(password);
        if (!users.findAll(u -> u.getUsername().equalsIgnoreCase(name)).isEmpty()) {
            throw new DuplicateEntityException("Username '" + name + "' is already taken.");
        }
        return users.save(new User(users.nextId(), name, PasswordUtil.hash(password), full, role));
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (currentUser == null) {
            throw new AuthenticationException("Not logged in.");
        }
        if (!PasswordUtil.verify(oldPassword == null ? "" : oldPassword, currentUser.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect.");
        }
        requireValidPassword(newPassword);
        currentUser.setPasswordHash(PasswordUtil.hash(newPassword));
        users.save(currentUser);
    }

    public List<User> listUsers() {
        return users.findAll();
    }

    private void requireValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
    }
}
