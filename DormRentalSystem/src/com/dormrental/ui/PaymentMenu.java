package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.enums.PaymentMethod;
import com.dormrental.enums.PaymentType;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.model.Lease;
import com.dormrental.model.Payment;
import com.dormrental.service.PaymentService;
import com.dormrental.util.MoneyUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PaymentMenu extends Menu {
    private record MenuEntry(String label, Runnable action) {}

    private final PaymentService service;
    private final List<MenuEntry> entries;

    public PaymentMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.service = ctx.getPaymentService();
        this.entries = List.of(
                new MenuEntry("Record a payment", this::record),
                new MenuEntry("View a tenant's bills", this::viewTenantBills),
                new MenuEntry("List overdue bills", this::listOverdue),
                new MenuEntry("Add utility / penalty charge", this::addCharge),
                new MenuEntry("Print a receipt", this::receipt),
                new MenuEntry("Check a tenant's balance", this::balance)
        );
    }

    @Override
    protected String title() {
        return "PAYMENTS";
    }

    @Override
    protected String[] options() {
        return entries.stream()
                .map(MenuEntry::label)
                .toArray(String[]::new);
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

    private void record() {
        String tenantId = in.readId("Tenant ID");
        if (ctx.getTenantService().getTenant(tenantId) == null) {
            Display.error("Tenant not found for ID: " + tenantId);
            return;
        }

        List<Payment> unpaid = service.listUnpaidByTenant(tenantId);
        if (unpaid == null || unpaid.isEmpty()) {
            Display.info("Nothing to pay - this tenant is all settled.");
            return;
        }

        Display.print(tables.payments(unpaid));
        String paymentId = in.readId("Payment ID to record");
        Payment payment = service.getPayment(paymentId);
        if (payment == null) {
            Display.error("Payment not found for ID: " + paymentId);
            return;
        }

        Display.info("Amount due: " + MoneyUtil.format(payment.getAmount()) + " (" + payment.getDescription() + ")");
        PaymentMethod method = in.readEnum("Payment method", PaymentMethod.class);
        String reference = blankToNull(in.readLine("Reference number (optional)"));

        try {
            service.recordPayment(paymentId, method, reference);
            Display.success("Payment recorded.");
            printReportOrFallback(ctx.getReportService().receipt(paymentId), "Receipt unavailable.");
        } catch (BusinessRuleException e) {
            Display.error(e.getMessage());
        }
    }

    private void viewTenantBills() {
        String tenantId = in.readId("Tenant ID");
        printReportOrFallback(
                ctx.getReportService().tenantLedger(tenantId, LocalDate.now()),
                "No bills found for tenant ID: " + tenantId
        );
    }

    private void listOverdue() {
        List<Payment> overdue = service.listOverdue();
        if (overdue == null || overdue.isEmpty()) {
            Display.info("No overdue bills.");
            return;
        }
        Display.print(tables.payments(overdue));
    }

    private void addCharge() {
        String tenantId = in.readId("Tenant ID");
        try {
            Lease lease = ctx.getLeaseService().findActiveLeaseByTenant(tenantId)
                    .orElseThrow(() -> new BusinessRuleException("That tenant has no active lease to bill against."));

            PaymentType type = in.readOption("Charge type", new PaymentType[]{PaymentType.UTILITY, PaymentType.PENALTY});
            String description = in.readRequired("Description (e.g. Electricity - October)");
            BigDecimal amount = in.readMoney("Amount (PHP)");
            LocalDate due = in.readDateOrDefault("Due date", LocalDate.now().plusDays(7));

            Payment payment = service.addCharge(lease.getId(), tenantId, type, description, amount, due);
            Display.success("Charge " + payment.getId() + " added.");
        } catch (BusinessRuleException e) {
            Display.error(e.getMessage());
        }
    }

    private void receipt() {
        String paymentId = in.readId("Payment ID");
        Payment payment = service.getPayment(paymentId);
        if (payment == null) {
            Display.error("Payment not found for ID: " + paymentId);
            return;
        }
        printReportOrFallback(ctx.getReportService().receipt(paymentId), "Receipt unavailable.");
    }

    private void balance() {
        String tenantId = in.readId("Tenant ID");
        String name = blankToNull(tables.tenantName(tenantId));
        String label = (name != null) ? name : tenantId;

        BigDecimal due = service.outstandingBalance(tenantId, LocalDate.now());
        BigDecimal safeDue = (due != null) ? due : BigDecimal.ZERO;
        Display.info(label + " owes " + MoneyUtil.format(safeDue) + " as of today.");
    }

    private static void printReportOrFallback(String content, String fallbackMessage) {
        if (content == null || content.isBlank()) {
            Display.info(fallbackMessage);
            return;
        }
        Display.print(content);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}