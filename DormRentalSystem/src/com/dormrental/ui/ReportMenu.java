package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.service.ReportService;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReportMenu extends Menu {
    private final ReportService service;

    public ReportMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.service = ctx.getReportService();
    }

    @Override
    protected String title() {
        return "REPORTS";
    }

    @Override
    protected String[] options() {
        return new String[]{
                "Occupancy by dormitory",
                "Monthly revenue",
                "Overdue payments",
                "Tenant ledger"
        };
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                Display.print(service.occupancyReport());
                break;
            case 2:
                revenue();
                break;
            case 3:
                Display.print(service.overdueReport(LocalDate.now()));
                break;
            case 4:
                ledger();
                break;
            default:
                break;
        }
    }

    private void revenue() {
        int year = in.readInt("Year", 2000, 2100);
        int month = in.readInt("Month", 1, 12);
        Display.print(service.revenueReport(YearMonth.of(year, month)));
    }

    private void ledger() {
        Display.print(tables.tenants(ctx.getTenantService().listTenants()));
        String tenantId = in.readId("Tenant ID");
        Display.print(service.tenantLedger(tenantId, LocalDate.now()));
    }
}