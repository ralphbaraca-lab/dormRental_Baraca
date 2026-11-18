package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.enums.PaymentMethod;
import com.dormrental.enums.PaymentType;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.model.Lease;
import com.dormrental.model.Payment;
import com.dormrental.model.Tenant;
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

    // Errors such as "tenant not found" or "payment already paid" are thrown by the services and
    // shown by Menu, so the methods below don't need their own try/catch or null checks.

    private void record() {
        String tenantId = in.readId("Tenant ID");
        Tenant tenant = ctx.getTenantService().getTenant(tenantId); // throws if there is no such tenant

        List<Payment> unpaid = service.listUnpaidByTenant(tenantId);
        if (unpaid.isEmpty()) {
            Display.info("Nothing to pay - this tenant is all settled.");
            return;
        }

        Display.print(tables.payments(unpaid));
        String paymentId = in.readId("Payment ID to record");
        // Only bills from the list above can be picked, so another tenant's bill can't be recorded by mistake.
        Payment payment = unpaid.stream()
                .filter(p -> p.getId().equals(paymentId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Payment " + paymentId + " is not one of " + tenant.getFullName() + "'s unpaid bills."));

        Display.info("Amount due: " + MoneyUtil.format(payment.getAmount()) + " (" + payment.getDescription() + ")");
        PaymentMethod method = in.readEnum("Payment method", PaymentMethod.class);
        String reference = blankToNull(in.readLine("Reference number (optional)"));

        // A recorded payment can't be undone from this menu, so ask first.
        if (!in.confirm("Record " + MoneyUtil.format(payment.getAmount()) + " paid by " + tenant.getFullName() + "?")) {
            Display.info("Cancelled.");
            return;
        }
        service.recordPayment(paymentId, method, reference);
        Display.success("Payment recorded.");
        Display.print(ctx.getReportService().receipt(paymentId));
    }

    private void viewTenantBills() {
        String tenantId = in.readId("Tenant ID");
        Display.print(ctx.getReportService().tenantLedger(tenantId, LocalDate.now()));
    }

    private void listOverdue() {
        List<Payment> overdue = service.listOverdue();
        if (overdue.isEmpty()) {
            Display.info("No overdue bills.");
            return;
        }
        Display.print(tables.payments(overdue));
    }

    private void addCharge() {
        String tenantId = in.readId("Tenant ID");
        Lease lease = ctx.getLeaseService().findActiveLeaseByTenant(tenantId)
                .orElseThrow(() -> new BusinessRuleException("That tenant has no active lease to bill against."));

        PaymentType type = in.readOption("Charge type", new PaymentType[]{PaymentType.UTILITY, PaymentType.PENALTY});
        String description = in.readRequired("Description (e.g. Electricity - October)");
        BigDecimal amount = in.readMoney("Amount (PHP)");
        LocalDate due = in.readDateOrDefault("Due date", LocalDate.now().plusDays(7));

        Payment payment = service.addCharge(lease.getId(), tenantId, type, description, amount, due);
        Display.success("Charge " + payment.getId() + " added.");
    }

    private void receipt() {
        String paymentId = in.readId("Payment ID");
        Display.print(ctx.getReportService().receipt(paymentId)); // throws if there is no such payment, or it isn't paid yet
    }

    private void balance() {
        String tenantId = in.readId("Tenant ID");
        Tenant tenant = ctx.getTenantService().getTenant(tenantId); // throws, instead of reporting "owes 0" for an unknown id
        BigDecimal due = service.outstandingBalance(tenantId, LocalDate.now());
        Display.info(tenant.getFullName() + " owes " + MoneyUtil.format(due) + " as of today.");
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}