package com.dormrental.model;

import com.dormrental.enums.PaymentMethod;
import com.dormrental.enums.PaymentStatus;
import com.dormrental.enums.PaymentType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One bill (deposit, monthly rent, utility or penalty) and, once settled, how it was paid. */
public class Payment implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String leaseId;
    private final String tenantId;
    private final PaymentType type;
    private final String description;
    private final BigDecimal amount;
    private final LocalDate dueDate;
    private PaymentStatus status = PaymentStatus.PENDING;
    private LocalDate paidDate;
    private PaymentMethod method;
    private String referenceNo;

    public Payment(String id, String leaseId, String tenantId, PaymentType type,
                   String description, BigDecimal amount, LocalDate dueDate) {
        this.id = id;
        this.leaseId = leaseId;
        this.tenantId = tenantId;
        this.type = type;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getLeaseId() {
        return leaseId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public PaymentType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public boolean isUnpaid() {
        return status != PaymentStatus.PAID;
    }

    public void markPaid(PaymentMethod method, LocalDate paidOn, String referenceNo) {
        this.status = PaymentStatus.PAID;
        this.method = method;
        this.paidDate = paidOn;
        this.referenceNo = referenceNo;
    }

    public long daysOverdue(LocalDate today) {
        if (!isUnpaid()) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, today));
    }
}
