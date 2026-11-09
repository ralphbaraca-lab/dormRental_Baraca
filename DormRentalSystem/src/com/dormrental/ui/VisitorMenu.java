package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.model.VisitorLog;
import com.dormrental.service.VisitorService;

public class VisitorMenu extends Menu {
    private final VisitorService service;

    public VisitorMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.service = ctx.getVisitorService();
    }

    @Override
    protected String title() {
        return "VISITOR LOG";
    }

    @Override
    protected String[] options() {
        return new String[]{
                "Check a visitor in",
                "Check a visitor out",
                "Visitors currently inside",
                "Visitor history for a tenant",
                "Full visitor history"
        };
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                checkIn();
                break;
            case 2:
                checkOut();
                break;
            case 3:
                Display.print(tables.visitors(service.listInside()));
                break;
            case 4:
                Display.print(tables.visitors(service.listByTenant(in.readId("Tenant ID"))));
                break;
            case 5:
                Display.print(tables.visitors(service.listAll()));
                break;
            default:
                break;
        }
    }

    private void checkIn() {
        String tenantId = in.readId("Tenant being visited (ID)");
        String visitor = in.readRequired("Visitor name");
        String purpose = in.readLine("Purpose (optional)");
        VisitorLog log = service.checkIn(tenantId, visitor, purpose);
        Display.success("Checked in. Log ID " + log.getId() + ".");
    }

    private void checkOut() {
        Display.print(tables.visitors(service.listInside()));
        String id = in.readId("Log ID to check out");
        service.checkOut(id);
        Display.success("Checked out.");
    }
}
