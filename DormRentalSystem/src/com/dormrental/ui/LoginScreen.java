package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.exception.AuthenticationException;
import com.dormrental.exception.DormRentalException;
import com.dormrental.model.User;
import com.dormrental.service.AuthService;
import com.dormrental.util.Validator;

public class LoginScreen {
    private static final int MAX_ATTEMPTS = 3;

    private final InputHelper in;
    private final AppContext ctx;

    public LoginScreen(InputHelper in, AppContext ctx) {
        this.in = in;
        this.ctx = ctx;
    }

    /** Returns true once authenticated, or false if the user exits or exceeds max attempts. */
    public boolean run() {
        Display.header("DORM RENTAL SYSTEM - LOGIN");
        Display.info("  First run: sign in as '" + AuthService.DEFAULT_ADMIN_USERNAME
                + "' / '" + AuthService.DEFAULT_ADMIN_PASSWORD
                + "', then change the password under Administration.");
        Display.info("  Leave Username blank to exit. Max attempts: " + MAX_ATTEMPTS + ".");

        int failedAttempts = 0;

        while (failedAttempts < MAX_ATTEMPTS) {
            String rawUsername = in.readLine("Username");
            if (Validator.isBlank(rawUsername)) {
                Display.info("Exiting login.");
                return false;
            }

            String username = rawUsername.trim();
            String rawPassword = in.readLine("Password");
            String password = rawPassword == null ? "" : rawPassword;

            // Password checking is deliberately slow now (PBKDF2), so tell the user something is happening.
            Display.info("  Checking credentials...");

            try {
                AuthService authService = ctx.getAuthService();
                authService.login(username, password);

                User currentUser = authService.getCurrentUser();
                String displayName = (currentUser != null && currentUser.getFullName() != null)
                        ? currentUser.getFullName()
                        : username;

                Display.success("Welcome, " + displayName + "!");
                return true;
            } catch (AuthenticationException e) {
                failedAttempts++;
                int remaining = MAX_ATTEMPTS - failedAttempts;
                Display.error(e.getMessage());
                if (remaining > 0) {
                    Display.info("  Attempts remaining: " + remaining);
                }
            } catch (DormRentalException e) {
                // Not a wrong password (e.g. hashing unavailable on this JVM), so it doesn't use up an attempt.
                Display.error("Login is unavailable: " + e.getMessage());
                return false;
            }
        }

        Display.error("Maximum login attempts reached. Returning to exit.");
        return false;
    }
}