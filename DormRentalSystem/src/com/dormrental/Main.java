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
    public static void main(String[] args) {
        String dataDir = args.length > 0 ? args[0] : "data";
        try {
            AppContext ctx = new AppContext(dataDir);
            InputHelper in = new InputHelper(new Scanner(System.in));
            LoginScreen login = new LoginScreen(in, ctx);

            while (login.run()) {
                Display.info(ctx.runDailyTasks());
                new MainMenu(in, ctx).run();
                ctx.getAuthService().logout();
                Display.info("Logged out.");
            }
            Display.info("Goodbye!");
        } catch (InputClosedException e) {
            System.out.println();
            Display.info("Input closed. Goodbye!");
        } catch (DormRentalException e) {
            Display.error(e.getMessage());
            System.exit(1);
        }
    }
}
