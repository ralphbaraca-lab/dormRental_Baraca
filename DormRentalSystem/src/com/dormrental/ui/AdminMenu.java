package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.config.DataSeeder;
import com.dormrental.enums.UserRole;
import com.dormrental.exception.ValidationException;
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

    // Errors thrown by the services are shown by Menu, which also keeps the stack trace of anything unexpected.
    // Catching RuntimeException here would hide bugs, and would print "null" when an exception has no message.

    private void runDailyTasks() {
        String summary = ctx.runDailyTasks();
        Display.success(summary == null || summary.isBlank() ? "Daily tasks completed." : summary);
    }

    private void addUser() {
        String username = in.readRequired("New username");
        String fullName = in.readRequired("Full name");
        String password = readNewPassword("Password (6+ characters)");
        UserRole role = in.readEnum("Role", UserRole.class);

        auth.registerUser(username, password, fullName, role);
        Display.success("Account '" + username + "' created.");
    }

    private void listUsers() {
        List<User> users = auth.listUsers();
        if (users.isEmpty()) {
            Display.info("No staff accounts found.");
            return;
        }
        Display.print(tables.users(users));
    }

    private void changePassword() {
        String oldPassword = readRequiredPassword("Current password");
        String newPassword = readNewPassword("New password (6+ characters)");

        auth.changePassword(oldPassword, newPassword);
        Display.success("Password changed.");
    }

    private void loadSampleData() {
        if (!in.confirm("Fill the system with made-up demo dormitories, tenants, leases and bills?")) {
            Display.info("Cancelled.");
            return;
        }
        DataSeeder.seed(ctx);
        Display.success("Sample data loaded.");
    }

    // ---- helpers -------------------------------------------------------------------------

    /** A password that can't be left empty. Typing is hidden when a real terminal is available. */
    private String readRequiredPassword(String prompt) {
        while (true) {
            String password = in.readPassword(prompt);
            if (!password.isEmpty()) {
                return password;
            }
            Display.error("This field is required.");
        }
    }

    /** Asks for a new password twice, so a typo can't lock someone out of an account they just made. */
    private String readNewPassword(String prompt) {
        String password = readRequiredPassword(prompt);
        String again = in.readPassword("Type it again to confirm");
        if (!password.equals(again)) {
            throw new ValidationException("The two passwords don't match.");
        }
        return password;
    }
}