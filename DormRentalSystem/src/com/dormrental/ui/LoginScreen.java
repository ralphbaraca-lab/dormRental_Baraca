package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.exception.AuthenticationException;
import com.dormrental.service.AuthService;

public class LoginScreen {
    private final InputHelper in;
    private final AppContext ctx;

    public LoginScreen(InputHelper in, AppContext ctx) {
        this.in = in;
        this.ctx = ctx;
    }

    /** Returns true once someone has logged in, or false if the user chose to exit. */
    public boolean run() {
        Display.header("DORM RENTAL SYSTEM - LOGIN");
        Display.info("  First run? Sign in as '" + AuthService.DEFAULT_ADMIN_USERNAME
                + "' with password '" + AuthService.DEFAULT_ADMIN_PASSWORD + "' and change it under Administration.");
        Display.info("  (Leave the username blank to exit.)");
        while (true) {
            String username = in.readLine("Username");
            if (username.isEmpty()) {
                return false;
            }
            String password = in.readLine("Password");
            try {
                ctx.getAuthService().login(username, password);
                Display.success("Welcome, " + ctx.getAuthService().getCurrentUser().getFullName() + "!");
                return true;
            } catch (AuthenticationException e) {
                Display.error(e.getMessage());
            }
        }
    }
}
