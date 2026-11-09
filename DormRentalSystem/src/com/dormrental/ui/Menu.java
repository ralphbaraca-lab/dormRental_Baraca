package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.exception.DormRentalException;
import com.dormrental.exception.InputClosedException;

/**
 * Base class for every menu screen. A subclass supplies a title, the option labels and what to
 * do for each choice; this class draws the menu, reads the choice and reports errors nicely.
 */
public abstract class Menu {
    protected final InputHelper in;
    protected final AppContext ctx;
    protected final Tables tables;

    protected Menu(InputHelper in, AppContext ctx) {
        this.in = in;
        this.ctx = ctx;
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
            Display.header(title());
            beforeOptions();
            String[] options = options();
            for (int i = 0; i < options.length; i++) {
                System.out.printf("  %d. %s%n", i + 1, options[i]);
            }
            System.out.printf("  0. %s%n", exitLabel());

            int choice = in.readInt("Choose", 0, options.length);
            if (choice == 0) {
                return;
            }
            try {
                handle(choice);
            } catch (InputClosedException e) {
                throw e;
            } catch (DormRentalException e) {
                Display.error(e.getMessage());
            } catch (RuntimeException e) {
                Display.error("Unexpected error: " + e);
            }
            if (pauseAfterAction()) {
                in.pause();
            }
        }
    }
}
