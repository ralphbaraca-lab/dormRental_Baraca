package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.config.DataSeeder;
import com.dormrental.enums.UserRole;
import com.dormrental.service.AuthService;

public class AdminMenu extends Menu {
    private final AuthService auth;

    public AdminMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.auth = ctx.getAuthService();
    }

    @Override
    protected String title() {
        return "ADMINISTRATION";
    }

    @Override
    protected String[] options() {
        return new String[]{
                "Run daily tasks now (close expired leases, flag overdue, late fees)",
                "Add staff account",
                "List staff accounts",
                "Change my password",
                "Load sample data (empty system only)"
        };
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                Display.success(ctx.runDailyTasks());
                break;
            case 2:
                addUser();
                break;
            case 3:
                Display.print(tables.users(auth.listUsers()));
                break;
            case 4:
                changePassword();
                break;
            case 5:
                loadSampleData();
                break;
            default:
                break;
        }
    }

    private void addUser() {
        String username = in.readRequired("New username");
        String fullName = in.readRequired("Full name");
        String password = in.readRequired("Password (6+ characters)");
        UserRole role = in.readEnum("Role", UserRole.class);
        auth.registerUser(username, password, fullName, role);
        Display.success("Account '" + username + "' created.");
    }

    private void changePassword() {
        String oldPassword = in.readRequired("Current password");
        String newPassword = in.readRequired("New password (6+ characters)");
        auth.changePassword(oldPassword, newPassword);
        Display.success("Password changed.");
    }

    private void loadSampleData() {
        if (in.confirm("Fill the system with made-up demo dormitories, tenants, leases and bills?")) {
            DataSeeder.seed(ctx);
            Display.success("Sample data loaded.");
        } else {
            Display.info("Cancelled.");
        }
    }
}
