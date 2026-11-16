package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.config.DataSeeder;
import com.dormrental.enums.UserRole;
import com.dormrental.model.User;
import com.dormrental.service.AuthService;
import java.util.List;

public class AdminMenu extends Menu {
    private record MenuEntry(String label, Runnable action) {}

    private final AuthService auth;
    private final List<MenuEntry> entries;

    public AdminMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.auth = ctx.getAuthService();
        this.entries = List.of(
                new MenuEntry("Run daily tasks now (close expired leases, flag overdue, late fees)", this::runDailyTasks),
                new MenuEntry("Add staff account", this::addUser),
                new MenuEntry("List staff accounts", this::listUsers),
                new MenuEntry("Change my password", this::changePassword),
                new MenuEntry("Load sample data (empty system only)", this::loadSampleData)
        );
    }

    @Override
    protected String title() {
        return "ADMINISTRATION";
    }

    @Override
    protected String[] options() {
        return entries.stream()
                .map(MenuEntry::label)
                .toArray(String[]::new);
    }

    @Override
    protected void handle(int choice) {
        int index = choice - 1;
        if (index < 0 || index >= entries.size()) {
            Display.error("Invalid menu selection.");
            return;
        }
        entries.get(index).action().run();
    }

    private void runDailyTasks() {
        try {
            String summary = ctx.runDailyTasks();
            if (summary == null || summary.isBlank()) {
                Display.success("Daily tasks completed.");
            } else {
                Display.success(summary);
            }
        } catch (RuntimeException e) {
            Display.error(e.getMessage());
        }
    }

    private void addUser() {
        String username = in.readRequired("New username").trim();
        String fullName = in.readRequired("Full name").trim();
        String password = in.readRequired("Password (6+ characters)");
        UserRole role = in.readEnum("Role", UserRole.class);

        try {
            auth.registerUser(username, password, fullName, role);
            Display.success("Account '" + username + "' created.");
        } catch (RuntimeException e) {
            Display.error(e.getMessage());
        }
    }

    private void listUsers() {
        List<User> users = auth.listUsers();
        if (users == null || users.isEmpty()) {
            Display.info("No staff accounts found.");
            return;
        }
        Display.print(tables.users(users));
    }

    private void changePassword() {
        String oldPassword = in.readRequired("Current password");
        String newPassword = in.readRequired("New password (6+ characters)");

        try {
            auth.changePassword(oldPassword, newPassword);
            Display.success("Password changed.");
        } catch (RuntimeException e) {
            Display.error(e.getMessage());
        }
    }

    private void loadSampleData() {
        if (!in.confirm("Fill the system with made-up demo dormitories, tenants, leases and bills?")) {
            Display.info("Cancelled.");
            return;
        }

        try {
            DataSeeder.seed(ctx);
            Display.success("Sample data loaded.");
        } catch (RuntimeException e) {
            Display.error(e.getMessage());
        }
    }
}