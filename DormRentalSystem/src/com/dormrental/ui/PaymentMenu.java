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
    private final PaymentService service;

    public PaymentMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.service = ctx.getPaymentService();
    }

    @Override
    protected String title() {
        return "PAYMENTS";
    }

    @Override
    protected String[] options() {
        return new String[]{
                "Record a payment",
                "View a tenant's bills",
                "List overdue bills",
                "Add utility / penalty charge",
                "Print a receipt",
                "Check a tenant's balance"
        };
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                record();
                break;
            case 2:
                viewTenantBills();
                break;
            case 3:
                Display.print(tables.payments(service.listOverdue()));
                break;
            case 4:
                addCharge();
                break;
            case 5:
                receipt();
                break;
            case 6:
                balance();
                break;
            default:
                break;
        }
    }

    private void record() {
        String tenantId = in.readId("Tenant ID");
        ctx.getTenantService().getTenant(tenantId);
        List<Payment> unpaid = service.listUnpaidByTenant(tenantId);
        Display.print(tables.payments(unpaid));
        if (unpaid.isEmpty()) {
            Display.info("Nothing to pay - this tenant is all settled.");
            return;
        }
        String paymentId = in.readId("Payment ID to record");
        Payment payment = service.getPayment(paymentId);
        Display.info("Amount due: " + MoneyUtil.format(payment.getAmount()) + " (" + payment.getDescription() + ")");
        PaymentMethod method = in.readEnum("Payment method", PaymentMethod.class);
        String reference = in.readLine("Reference number (optional)");
        service.recordPayment(paymentId, method, reference);
        Display.success("Payment recorded.");
        Display.print(ctx.getReportService().receipt(paymentId));
    }

    private void viewTenantBills() {
        String tenantId = in.readId("Tenant ID");
        Display.print(ctx.getReportService().tenantLedger(tenantId, LocalDate.now()));
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
        Display.print(ctx.getReportService().receipt(paymentId));
    }

    private void balance() {
        String tenantId = in.readId("Tenant ID");
        String name = tables.tenantName(tenantId);
        BigDecimal due = service.outstandingBalance(tenantId, LocalDate.now());
        Display.info(name + " owes " + MoneyUtil.format(due) + " as of today.");
    }
}
