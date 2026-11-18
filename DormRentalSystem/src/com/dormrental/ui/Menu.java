
package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.exception.DormRentalException;
import com.dormrental.exception.InputClosedException;
import java.util.Objects;

/**
 * Base class for all menu screens in the Dorm Rental System.
 *
 * Subclasses provide their own title, menu options,
 * and action handling through abstract methods.
 */
public abstract class Menu {

    protected final InputHelper in;
    protected final AppContext ctx;
    protected final Tables tables;

    // Constructor
    protected Menu(InputHelper in, AppContext ctx) {

        this.in = Objects.requireNonNull(
            in, "InputHelper cannot be null."
        );

        this.ctx = Objects.requireNonNull(
            ctx, "AppContext cannot be null."
        );

        this.tables = new Tables(ctx);
    }

    // Returns the menu title
    protected abstract String title();

    // Returns available menu options
    protected abstract String[] options();

    /**
     * Handles the selected menu option.
     *
     * @param choice selected option number (1 to n)
     */
    protected abstract void handle(int choice);

    // Returns the exit option label
    protected String exitLabel() {
        return "Back";
    }

    // Hook for displaying additional information before menu options
    protected void beforeOptions() {
        // Optional implementation by subclasses
    }

    // Determines whether to pause after executing an action
    protected boolean pauseAfterAction() {
        return true;
    }

    /**
     * Starts the menu loop.
     * Continues displaying options until the user selects exit.
     */
    public final void run() {

        while (true) {

            String[] menuOptions = options();

            validateOptions(menuOptions);

            show(menuOptions);

            int choice = in.readInt(
                "Choose", 0, menuOptions.length
            );

            // Exit or return to previous menu
            if (choice == 0) {
                return;
            }

            perform(choice);

            // Pause before displaying the menu again
            if (pauseAfterAction()) {
                in.pause();
            }
        }
    }

    /**
     * Validates menu options before displaying them.
     */
    private void validateOptions(String[] options) {

        Objects.requireNonNull(
            options, "Menu options cannot be null."
        );

        if (options.length == 0) {
            throw new IllegalStateException(
                "A menu must contain at least one option."
            );
        }

        for (String option : options) {

            if (option == null || option.isBlank()) {
                throw new IllegalStateException(
                    "Menu options cannot contain empty labels."
                );
            }
        }
    }

    /**
     * Displays the menu title, additional information,
     * and available options.
     */
    private void show(String[] options) {

        Display.header(title());

        beforeOptions();

        for (int i = 0; i < options.length; i++) {

            Display.println(
                "  " + (i + 1) + ". " + options[i]
            );
        }

        Display.println("  0. " + exitLabel());
    }

    /**
     * Executes the selected menu action.
     * Handles application errors without terminating the menu.
     */
    private void perform(int choice) {

        try {

            handle(choice);

        } catch (InputClosedException e) {

            // Propagate closed input to the application.
            throw e;

        } catch (DormRentalException e) {

            // Expected application or business errors.
            Display.error(e.getMessage());

        } catch (RuntimeException e) {

            // Unexpected programming or runtime errors.
            Display.error(
                "An unexpected error occurred. " +
                "Please contact the system administrator."
            );

            // Preserve technical details for debugging.
            e.printStackTrace();
        }
    }
}
