package com.dormrental.ui;

import com.dormrental.config.AppContext;

public class MainMenu extends Menu {

    public MainMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
    }

    @Override
    protected String title() {
        return "MAIN MENU  (logged in as " + ctx.getAuthService().getCurrentUser().getUsername() + ")";
    }

    @Override
    protected void beforeOptions() {
        Display.println(ctx.getReportService().dashboard());
        Display.println("");
    }

    @Override
    protected String[] options() {
        return new String[]{
                "Dormitories",
                "Rooms",
                "Tenants",
                "Leases",
                "Payments",
                "Maintenance requests",
                "Visitor log",
                "Reports",
                "Administration"
        };
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
        switch (choice) {
            case 1:
                new DormitoryMenu(in, ctx).run();
                break;
            case 2:
                new RoomMenu(in, ctx).run();
                break;
            case 3:
                new TenantMenu(in, ctx).run();
                break;
            case 4:
                new LeaseMenu(in, ctx).run();
                break;
            case 5:
                new PaymentMenu(in, ctx).run();
                break;
            case 6:
                new MaintenanceMenu(in, ctx).run();
                break;
            case 7:
                new VisitorMenu(in, ctx).run();
                break;
            case 8:
                new ReportMenu(in, ctx).run();
                break;
            case 9:
                new AdminMenu(in, ctx).run();
                break;
            default:
                break;
        }
    }
}
