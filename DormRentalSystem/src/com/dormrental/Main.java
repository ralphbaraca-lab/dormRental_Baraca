package com.dormrental;

import com.dormrental.config.AppContext;
import com.dormrental.exception.DormRentalException;
import com.dormrental.exception.InputClosedException;
import com.dormrental.ui.Display;
import com.dormrental.ui.InputHelper;
import com.dormrental.ui.LoginScreen;
import com.dormrental.ui.MainMenu;
import java.util.Scanner;

/**
 * Entry point. Optional argument: the folder where data files are kept (default "data").
 */
public class Main {
    private static final String DEFAULT_DATA_DIR = "data";

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /** Runs the whole program and returns the exit code: 0 for a normal exit, 1 if it had to stop on an error. */
    private static int run(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            AppContext ctx = new AppContext(dataDirFrom(args));
            InputHelper in = new InputHelper(scanner);
            LoginScreen login = new LoginScreen(in, ctx);

            while (login.run()) {
                runDailyTasks(ctx);
                runSession(in, ctx);
            }
            Display.info("Goodbye!");
            return 0;
        } catch (InputClosedException e) {
            System.out.println();
            Display.info("Input closed. Goodbye!");
            return 0;
        } catch (DormRentalException e) {
            Display.error(e.getMessage());
            return 1;
        }
    }

    /** A missing or blank argument means the default folder, instead of quietly using the current one. */
    private static String dataDirFrom(String[] args) {
        return args.length > 0 && !args[0].isBlank() ? args[0].trim() : DEFAULT_DATA_DIR;
    }

    /**
     * A problem with the daily tasks is reported but doesn't stop anyone from signing in,
     * otherwise one bad record could lock everybody out. They can be run again from the Administration menu.
     */
    private static void runDailyTasks(AppContext ctx) {
        try {
            Display.info(ctx.runDailyTasks());
        } catch (DormRentalException e) {
            Display.error("Daily tasks could not run: " + e.getMessage());
        } catch (RuntimeException e) {
            Display.error("Daily tasks failed unexpectedly: " + e);
            e.printStackTrace();
        }
    }

    private static void runSession(InputHelper in, AppContext ctx) {
        try {
            new MainMenu(in, ctx).run();
        } finally {
            ctx.getAuthService().logout(); // whatever ended the session, nobody stays signed in
        }
        Display.info("Logged out.");
    }
}