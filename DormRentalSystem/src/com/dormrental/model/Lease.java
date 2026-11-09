package com.dormrental.model;

import com.dormrental.enums.LeaseStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A rental agreement between one tenant and one room. endDate is the last day covered. */
public class Lease implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String tenantId;
    private final String roomId;
    private final LocalDate startDate;
    private LocalDate endDate;
    private final BigDecimal monthlyRate;
    private final BigDecimal securityDeposit;
    private LeaseStatus status = LeaseStatus.ACTIVE;
    private LocalDate terminatedOn;
    private String terminationReason;

    public Lease(String id, String tenantId, String roomId, LocalDate startDate, LocalDate endDate,
                 BigDecimal monthlyRate, BigDecimal securityDeposit) {
        this.id = id;
        this.tenantId = tenantId;
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRate = monthlyRate;
        this.securityDeposit = securityDeposit;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRoomId() {
        return roomId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMonthlyRate() {
        return monthlyRate;
    }

    public BigDecimal getSecurityDeposit() {
        return securityDeposit;
    }

    public LeaseStatus getStatus() {
        return status;
    }

    public LocalDate getTerminatedOn() {
        return terminatedOn;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public boolean isActive() {
        return status == LeaseStatus.ACTIVE;
    }

    /** Marks the lease as finished normally (it reached its end date). */
    public void end() {
        status = LeaseStatus.ENDED;
    }

    /** Marks the lease as cut short. */
    public void terminate(LocalDate on, String reason) {
        status = LeaseStatus.TERMINATED;
        terminatedOn = on;
        terminationReason = reason;
    }

    public long daysUntilEnd(LocalDate today) {
        return ChronoUnit.DAYS.between(today, endDate);
    }
}
