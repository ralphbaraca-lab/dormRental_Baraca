package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.model.User;
import java.util.List;

public class MainMenu extends Menu {

    private record MenuEntry(String label, Runnable action) {}

    private final List<MenuEntry> entries;

    public MainMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.entries = List.of(
                new MenuEntry("Dormitories", () -> new DormitoryMenu(in, ctx).run()),
                new MenuEntry("Rooms", () -> new RoomMenu(in, ctx).run()),
                new MenuEntry("Tenants", () -> new TenantMenu(in, ctx).run()),
                new MenuEntry("Leases", () -> new LeaseMenu(in, ctx).run()),
                new MenuEntry("Payments", () -> new PaymentMenu(in, ctx).run()),
                new MenuEntry("Maintenance requests", () -> new MaintenanceMenu(in, ctx).run()),
                new MenuEntry("Visitor log", () -> new VisitorMenu(in, ctx).run()),
                new MenuEntry("Reports", () -> new ReportMenu(in, ctx).run()),
                new MenuEntry("Administration", () -> new AdminMenu(in, ctx).run())
        );
    }

    @Override
    protected String title() {
        User currentUser = ctx.getAuthService().getCurrentUser();
        String username = (currentUser != null && currentUser.getUsername() != null)
                ? currentUser.getUsername()
                : "unknown";
        return "MAIN MENU  (logged in as " + username + ")";
    }

    @Override
    protected void beforeOptions() {
        String summary = ctx.getReportService().dashboard();
        if (summary != null && !summary.isBlank()) {
            Display.println(summary);
            Display.println("");
        }
    }

    @Override
    protected String[] options() {
        return entries.stream()
                .map(MenuEntry::label)
                .toArray(String[]::new);
    }

    @Override
    protected String exitLabel() {
        return "Log out";
    }

    @Override
    protected boolean pauseAfterAction() {
        return false;
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
}