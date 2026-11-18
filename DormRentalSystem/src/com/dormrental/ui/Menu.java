package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.exception.DormRentalException;
import com.dormrental.exception.InputClosedException;
import java.util.Objects;

/**
 * Base class for every menu screen. A subclass supplies a title, the option labels and what to
 * do for each choice; this class draws the menu, reads the choice and reports errors nicely.
 */
public abstract class Menu {
    protected final InputHelper in;
    protected final AppContext ctx;
    protected final Tables tables;

    protected Menu(InputHelper in, AppContext ctx) {
        this.in = Objects.requireNonNull(in, "in must not be null");
        this.ctx = Objects.requireNonNull(ctx, "ctx must not be null");
        this.tables = new Tables(ctx);
    }

    protected abstract String title();

    protected abstract String[] options();

    /** Called with the number (1..n) of the option the user picked. */
    protected abstract void handle(int choice);

    /** Label for option 0. */
    protected String exitLabel() {
        return "Back";
    }

    /** Hook for menus that want to print something above the options. */
    protected void beforeOptions() {
    }

    /** Menus that only open other menus return false so the user is not asked to press Enter on the way back. */
    protected boolean pauseAfterAction() {
        return true;
    }

    public final void run() {
        while (true) {
            String[] options = options();
            show(options);

            int choice = in.readInt("Choose", 0, options.length);
            if (choice == 0) {
                return;
            }
            perform(choice);
            if (pauseAfterAction()) {
                in.pause();
            }
        }
    }

    private void show(String[] options) {
        Display.header(title());
        beforeOptions();
        for (int i = 0; i < options.length; i++) {
            Display.println("  " + (i + 1) + ". " + options[i]);
        }
        Display.println("  0. " + exitLabel());
    }

    /** Runs one choice, so that a failure in it shows a message and returns to the menu instead of ending the program. */
    private void perform(int choice) {
        try {
            handle(choice);
        } catch (InputClosedException e) {
            throw e; // the keyboard input has ended, so there is nobody left to show a message to
        } catch (DormRentalException e) {
            Display.error(e.getMessage());
        } catch (RuntimeException e) {
            // Not an expected business error, so it is probably a bug: keep the stack trace for whoever fixes it.
            Display.error("Unexpected error: " + e);
            e.printStackTrace();
        }
    }
}