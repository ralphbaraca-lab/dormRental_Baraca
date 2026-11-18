
package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.exception.AuthenticationException;
import com.dormrental.exception.DormRentalException;
import com.dormrental.model.User;
import com.dormrental.service.AuthService;
import com.dormrental.util.Validator;
import java.util.Objects;

/**
 * Handles user login and authentication through the console.
 */
public class LoginScreen {

    private static final int MAX_ATTEMPTS = 3;

    private final InputHelper in;
    private final AppContext ctx;

    // Constructor
    public LoginScreen(InputHelper in, AppContext ctx) {
        this.in = Objects.requireNonNull(in, "InputHelper cannot be null.");
        this.ctx = Objects.requireNonNull(ctx, "AppContext cannot be null.");
    }

    /**
     * Starts the login process.
     *
     * @return true if authentication succeeds;
     *         false if the user exits or exceeds login attempts.
     */
    public boolean run() {

        Display.header("DORM RENTAL SYSTEM - LOGIN");

        Display.info("  Please enter your administrator or registered account credentials.");
        Display.info("  Leave Username blank to exit.");
        Display.info("  Maximum login attempts: " + MAX_ATTEMPTS + ".");

        AuthService authService = ctx.getAuthService();

        int failedAttempts = 0;

        while (failedAttempts < MAX_ATTEMPTS) {

            // Read username
            String rawUsername = in.readLine("Username");

            if (Validator.isBlank(rawUsername)) {
                Display.info("Login cancelled. Exiting login.");
                return false;
            }

            String username = rawUsername.trim();

            // Read password
            String password = in.readPassword("Password");

            try {

                Display.info("Checking credentials...");

                // Authenticate user
                User user = authService.login(username, password);

                // Successful authentication
                Display.success(
                    "Welcome, " + displayName(user, username) + "!"
                );

                return true;

            } catch (AuthenticationException e) {

                // Invalid credentials
                failedAttempts++;

                Display.error("Invalid username or password.");

                int remainingAttempts = MAX_ATTEMPTS - failedAttempts;

                if (remainingAttempts > 0) {
                    Display.info(
                        "Attempts remaining: " + remainingAttempts
                    );
                }

            } catch (DormRentalException e) {

                // Authentication service or application error
                Display.error(
                    "Login service is currently unavailable. " +
                    "Please contact the administrator."
                );

                return false;

            } finally {

                // Release the password reference after authentication.
                // Java Strings cannot be securely erased from memory.
                password = null;
            }
        }

        // Maximum attempts reached
        Display.error(
            "Maximum login attempts reached. Returning to exit."
        );

        return false;
    }

    /**
     * Returns the user's full name.
     * Uses the username if the full name is unavailable.
     */
    private static String displayName(User user, String fallback) {

        if (user == null) {
            return fallback;
        }

        String fullName = user.getFullName();

        return Validator.isBlank(fullName)
                ? fallback
                : fullName.trim();
    }
}
