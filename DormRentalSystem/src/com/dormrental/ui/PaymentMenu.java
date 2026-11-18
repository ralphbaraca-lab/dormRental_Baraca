
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
import java.util.Objects;

public class PaymentMenu extends Menu {

    private record MenuEntry(String label, Runnable action) {
        private MenuEntry {
            Objects.requireNonNull(label, "Menu label cannot be null.");
            Objects.requireNonNull(action, "Menu action cannot be null.");

            if (label.isBlank()) {
                throw new IllegalArgumentException("Menu label cannot be blank.");
            }
        }
    }

    private final PaymentService service;
    private final List<MenuEntry> entries;

    public PaymentMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);

        this.service = Objects.requireNonNull(
                ctx.getPaymentService(),
                "PaymentService cannot be null."
        );

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
        if (choice < 1 || choice > entries.size()) {
            Display.error("Invalid menu selection.");
            return;
        }

        MenuEntry selected = entries.get(choice - 1);
        selected.action().run();
    }

    // Records an unpaid bill as paid.
    private void record() {
        String tenantId = in.readId("Tenant ID");

        Tenant tenant = ctx.getTenantService().getTenant(tenantId);

        List<Payment> unpaidPayments = service.listUnpaidByTenant(tenantId);

        if (unpaidPayments.isEmpty()) {
            Display.info("This tenant has no unpaid bills.");
            return;
        }

        Display.subheader("UNPAID BILLS");
        Display.print(tables.payments(unpaidPayments));

        String paymentId = in.readId("Payment ID to record");

        Payment payment = findUnpaidPayment(
                unpaidPayments,
                paymentId,
                tenant.getFullName()
        );

        Display.subheader("PAYMENT DETAILS");
        Display.info("Tenant: " + tenant.getFullName());
        Display.info("Description: " + payment.getDescription());
        Display.info("Amount due: " + MoneyUtil.format(payment.getAmount()));

        PaymentMethod method = in.readEnum(
                "Payment method",
                PaymentMethod.class
        );

        String reference = blankToNull(
                in.readLine("Reference number (optional)")
        );

        if (!in.confirm(
                "Confirm payment of " +
                MoneyUtil.format(payment.getAmount()) +
                " for " + tenant.getFullName() + "?"
        )) {
            Display.info("Payment recording cancelled.");
            return;
        }

        service.recordPayment(paymentId, method, reference);

        Display.success("Payment successfully recorded.");

        Display.subheader("PAYMENT RECEIPT");
        Display.print(ctx.getReportService().receipt(paymentId));
    }

    // Finds a payment only within the selected tenant's unpaid bills.
    private Payment findUnpaidPayment(
            List<Payment> unpaidPayments,
            String paymentId,
            String tenantName
    ) {
        return unpaidPayments.stream()
                .filter(payment -> payment.getId().equals(paymentId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Payment ID " + paymentId +
                        " does not belong to " + tenantName +
                        "'s unpaid bills."
                ));
    }

    // Displays a tenant's payment history and bills.
    private void viewTenantBills() {
        String tenantId = in.readId("Tenant ID");

        String ledger = ctx.getReportService()
                .tenantLedger(tenantId, LocalDate.now());

        Display.subheader("TENANT PAYMENT LEDGER");
        Display.print(ledger);
    }

    // Displays all currently overdue payments.
    private void listOverdue() {
        List<Payment> overduePayments = service.listOverdue();

        if (overduePayments.isEmpty()) {
            Display.info("There are no overdue bills.");
            return;
        }

        Display.subheader("OVERDUE BILLS");
        Display.print(tables.payments(overduePayments));
    }

    // Adds a utility or penalty charge to an active lease.
    private void addCharge() {
        String tenantId = in.readId("Tenant ID");

        Lease lease = ctx.getLeaseService()
                .findActiveLeaseByTenant(tenantId)
                .orElseThrow(() -> new BusinessRuleException(
                        "This tenant has no active lease to bill against."
                ));

        PaymentType type = in.readOption(
                "Charge type",
                new PaymentType[]{
                        PaymentType.UTILITY,
                        PaymentType.PENALTY
                }
        );

        String description = in.readRequired(
                "Charge description"
        );

        BigDecimal amount = in.readMoney("Amount (PHP)");

        LocalDate dueDate = in.readDateOrDefault(
                "Due date",
                LocalDate.now().plusDays(7)
        );

        Payment payment = service.addCharge(
                lease.getId(),
                tenantId,
                type,
                description,
                amount,
                dueDate
        );

        Display.success(
                "Charge successfully added. Payment ID: " +
                payment.getId()
        );
    }

    // Prints a receipt for an already-paid payment.
    private void receipt() {
        String paymentId = in.readId("Payment ID");

        String receipt = ctx.getReportService()
                .receipt(paymentId);

        Display.subheader("PAYMENT RECEIPT");
        Display.print(receipt);
    }

    // Displays the tenant's outstanding balance.
    private void balance() {
        String tenantId = in.readId("Tenant ID");

        Tenant tenant = ctx.getTenantService()
                .getTenant(tenantId);

        BigDecimal outstanding = service.outstandingBalance(
                tenantId,
                LocalDate.now()
        );

        Display.subheader("TENANT BALANCE");

        Display.info("Tenant: " + tenant.getFullName());
        Display.info("Outstanding balance: " + MoneyUtil.format(outstanding));
        Display.info("Date: " + LocalDate.now());
    }

    // Converts blank optional input into null.
    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
