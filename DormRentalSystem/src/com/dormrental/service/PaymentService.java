package com.dormrental.service;

import com.dormrental.enums.PaymentMethod;
import com.dormrental.enums.PaymentStatus;
import com.dormrental.enums.PaymentType;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.exception.EntityNotFoundException;
import com.dormrental.exception.ValidationException;
import com.dormrental.model.Lease;
import com.dormrental.model.Payment;
import com.dormrental.repository.Repository;
import com.dormrental.util.Validator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class PaymentService {
    /** Late fee charged once on each overdue rent bill: 5% of the rent. */
    public static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.05");

    /** A late fee's description is this prefix plus the id of the rent bill it belongs to. */
    private static final String LATE_FEE_PREFIX = "Late fee for ";

    private static final Comparator<Payment> BY_DUE_DATE =
            Comparator.comparing(Payment::getDueDate).thenComparing(Payment::getId);

    private final Repository<Payment> payments;

    public PaymentService(Repository<Payment> payments) {
        this.payments = payments;
    }

    // ---- billing -------------------------------------------------------------------------

    /** Creates the security deposit bill and one rent bill per month of the lease. */
    public void generateSchedule(Lease lease) {
        if (lease.getSecurityDeposit().signum() > 0) {
            payments.save(new Payment(payments.nextId(), lease.getId(), lease.getTenantId(),
                    PaymentType.DEPOSIT, "Security deposit", lease.getSecurityDeposit(), lease.getStartDate()));
        }
        int months = (int) ChronoUnit.MONTHS.between(lease.getStartDate(), lease.getEndDate().plusDays(1));
        generateRentPayments(lease, lease.getStartDate(), months);
    }

    /** Creates {@code months} monthly rent bills starting on {@code firstDue}. Also used when a lease is renewed. */
    public void generateRentPayments(Lease lease, LocalDate firstDue, int months) {
        for (int i = 0; i < months; i++) {
            LocalDate due = firstDue.plusMonths(i); // counted from firstDue, so a 31st rolls back to the month's last day
            payments.save(new Payment(payments.nextId(), lease.getId(), lease.getTenantId(),
                    PaymentType.RENT, "Rent for " + monthLabel(due), lease.getMonthlyRate(), due));
        }
    }

    /** Adds a one-off utility or penalty charge to a tenant's account. */
    public Payment addCharge(String leaseId, String tenantId, PaymentType type, String description,
                             BigDecimal amount, LocalDate dueDate) {
        if (type != PaymentType.UTILITY && type != PaymentType.PENALTY) {
            throw new ValidationException("Manual charges must be UTILITY or PENALTY.");
        }
        String text = Validator.requireNotBlank(description, "Description");
        Validator.requirePositive(amount, "Amount");
        if (dueDate == null) {
            throw new ValidationException("Due date is required.");
        }
        return payments.save(new Payment(payments.nextId(), leaseId, tenantId, type, text, amount, dueDate));
    }

    // ---- collecting ----------------------------------------------------------------------

    public Payment recordPayment(String paymentId, PaymentMethod method, String referenceNo) {
        if (method == null) {
            throw new ValidationException("Payment method is required.");
        }
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BusinessRuleException("Payment " + paymentId + " has already been paid.");
        }
        String ref = referenceNo == null ? "" : referenceNo.trim();
        payment.markPaid(method, LocalDate.now(), ref);
        return payments.save(payment);
    }

    // ---- housekeeping --------------------------------------------------------------------

    /** Flags unpaid bills whose due date has passed. Returns how many changed. */
    public int refreshOverdue(LocalDate today) {
        int changed = 0;
        for (Payment payment : payments.findAll(p -> p.getStatus() == PaymentStatus.PENDING && p.getDueDate().isBefore(today))) {
            payment.setStatus(PaymentStatus.OVERDUE);
            payments.save(payment);
            changed++;
        }
        return changed;
    }

    /** Adds one late-fee bill for every overdue rent bill that has not been charged yet. */
    public int applyLateFees(LocalDate today) {
        List<Payment> overdueRent = payments.findAll(p -> p.getType() == PaymentType.RENT
                && p.getStatus() == PaymentStatus.OVERDUE);

        // Read the existing penalties once, instead of searching all payments again for every overdue bill.
        Set<String> charged = payments.findAll(p -> p.getType() == PaymentType.PENALTY).stream()
                .map(Payment::getDescription)
                .collect(Collectors.toSet());

        int created = 0;
        for (Payment rent : overdueRent) {
            String description = LATE_FEE_PREFIX + rent.getId();
            if (!charged.add(description)) {
                continue; // already has a late fee
            }
            BigDecimal fee = rent.getAmount().multiply(LATE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
            payments.save(new Payment(payments.nextId(), rent.getLeaseId(), rent.getTenantId(),
                    PaymentType.PENALTY, description, fee, today));
            created++;
        }
        return created;
    }

    /** Removes rent bills that fall after {@code after} and have not been paid (used when a lease is terminated). */
    public int cancelFutureDues(String leaseId, LocalDate after) {
        List<Payment> toCancel = payments.findAll(p -> p.getLeaseId().equals(leaseId)
                && p.getType() == PaymentType.RENT
                && p.getStatus() == PaymentStatus.PENDING
                && p.getDueDate().isAfter(after));
        for (Payment payment : toCancel) {
            payments.deleteById(payment.getId());
        }
        return toCancel.size();
    }

    // ---- queries -------------------------------------------------------------------------

    public Payment getPayment(String id) {
        return payments.findById(id).orElseThrow(() -> new EntityNotFoundException("Payment", id));
    }

    public List<Payment> listAll() {
        return byDueDate(payments.findAll());
    }

    public List<Payment> listByLease(String leaseId) {
        return byDueDate(payments.findAll(p -> p.getLeaseId().equals(leaseId)));
    }

    public List<Payment> listByTenant(String tenantId) {
        return byDueDate(payments.findAll(p -> p.getTenantId().equals(tenantId)));
    }

    public List<Payment> listUnpaidByTenant(String tenantId) {
        return byDueDate(payments.findAll(p -> p.getTenantId().equals(tenantId) && p.isUnpaid()));
    }

    public List<Payment> listOverdue() {
        return byDueDate(payments.findAll(p -> p.getStatus() == PaymentStatus.OVERDUE));
    }

    /** Sum of unpaid bills that are already due on or before {@code asOf}. */
    public BigDecimal outstandingBalance(String tenantId, LocalDate asOf) {
        return payments.findAll(p -> p.getTenantId().equals(tenantId) && p.isUnpaid()
                        && !p.getDueDate().isAfter(asOf)).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---- helpers -------------------------------------------------------------------------

    private static String monthLabel(LocalDate date) {
        return date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getYear();
    }

    /** Sorts a copy, so we never reorder (or fail on) a list the repository may be holding on to. */
    private static List<Payment> byDueDate(List<Payment> list) {
        List<Payment> sorted = new ArrayList<>(list);
        sorted.sort(BY_DUE_DATE);
        return sorted;
    }
}