
package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.service.ReportService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public class ReportMenu extends Menu {

    private static final String[] OPTIONS = {
            "Occupancy by dormitory",
            "Monthly revenue",
            "Overdue payments",
            "Tenant ledger"
    };

    private final ReportService service;

    public ReportMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);

        this.service = Objects.requireNonNull(
                ctx.getReportService(),
                "ReportService cannot be null."
        );
    }

    @Override
    protected String title() {
        return "REPORTS";
    }

    @Override
    protected String[] options() {
        return OPTIONS.clone();
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                occupancy();
                break;

            case 2:
                revenue();
                break;

            case 3:
                overdue();
                break;

            case 4:
                ledger();
                break;

            default:
                Display.error("Invalid menu selection.");
                break;
        }
    }

    // Displays occupancy information grouped by dormitory.
    private void occupancy() {
        Display.subheader("DORMITORY OCCUPANCY REPORT");

        String report = service.occupancyReport();

        Display.print(report);
    }

    // Generates a revenue report for a selected month.
    private void revenue() {
        Display.subheader("MONTHLY REVENUE REPORT");

        int year = in.readInt(
                "Year",
                2000,
                2100
        );

        int month = in.readInt(
                "Month",
                1,
                12
        );

        YearMonth selectedMonth = YearMonth.of(year, month);

        Display.info("Selected period: " + selectedMonth);

        if (!in.confirm("Generate revenue report for " + selectedMonth + "?")) {
            Display.info("Report generation cancelled.");
            return;
        }

        String report = service.revenueReport(selectedMonth);

        Display.print(report);
    }

    // Displays overdue payment information as of today.
    private void overdue() {
        Display.subheader("OVERDUE PAYMENTS REPORT");

        LocalDate reportDate = LocalDate.now();

        Display.info("Report date: " + reportDate);

        String report = service.overdueReport(reportDate);

        Display.print(report);
    }

    // Displays the payment ledger of a selected tenant.
    private void ledger() {
        Display.subheader("TENANT PAYMENT LEDGER");

        if (ctx.getTenantService().listTenants().isEmpty()) {
            Display.info("No tenants are currently registered.");
            return;
        }

        Display.print(
                tables.tenants(ctx.getTenantService().listTenants())
        );

        String tenantId = in.readId("Tenant ID");

        LocalDate reportDate = LocalDate.now();

        String report = service.tenantLedger(
                tenantId,
                reportDate
        );

        Display.print(report);
    }
}
