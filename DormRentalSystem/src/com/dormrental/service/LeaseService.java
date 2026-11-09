package com.dormrental.service;

import com.dormrental.enums.LeaseStatus;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.exception.EntityNotFoundException;
import com.dormrental.exception.ValidationException;
import com.dormrental.model.Dormitory;
import com.dormrental.model.Lease;
import com.dormrental.model.Room;
import com.dormrental.model.Tenant;
import com.dormrental.repository.Repository;
import com.dormrental.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class LeaseService {
    public static final int MAX_LEASE_MONTHS = 24;

    private final Repository<Lease> leases;
    private final TenantService tenantService;
    private final RoomService roomService;
    private final DormitoryService dormitoryService;
    private final PaymentService paymentService;

    public LeaseService(Repository<Lease> leases, TenantService tenantService, RoomService roomService,
                        DormitoryService dormitoryService, PaymentService paymentService) {
        this.leases = leases;
        this.tenantService = tenantService;
        this.roomService = roomService;
        this.dormitoryService = dormitoryService;
        this.paymentService = paymentService;
    }

    /** Moves a tenant into a room, signs the lease and generates the billing schedule. */
    public Lease createLease(String tenantId, String roomId, LocalDate startDate, int months, BigDecimal securityDeposit) {
        Tenant tenant = tenantService.getTenant(tenantId);
        if (!tenant.isActive()) {
            throw new BusinessRuleException(tenant.getFullName() + " is deactivated. Reactivate the tenant first.");
        }
        if (findActiveLeaseByTenant(tenantId).isPresent()) {
            throw new BusinessRuleException(tenant.getFullName() + " already has an active lease.");
        }
        Room room = roomService.getRoom(roomId);
        Dormitory dormitory = dormitoryService.getDormitory(room.getDormitoryId());
        if (!dormitory.getType().accepts(tenant.getGender())) {
            throw new BusinessRuleException(dormitory.getName() + " is " + dormitory.getType().getLabel().toLowerCase()
                    + " and cannot house this tenant.");
        }
        if (startDate == null) {
            throw new ValidationException("Start date is required.");
        }
        requireValidMonths(months);
        if (securityDeposit == null || securityDeposit.signum() < 0) {
            throw new ValidationException("Security deposit cannot be negative.");
        }
        LocalDate endDate = startDate.plusMonths(months).minusDays(1);
        if (endDate.isBefore(LocalDate.now())) {
            throw new ValidationException("That lease would already be over (it ends " + endDate + ").");
        }

        roomService.assignOccupant(roomId, tenantId); // checks vacancy and maintenance mode
        Lease lease = new Lease(leases.nextId(), tenantId, roomId, startDate, endDate,
                room.getMonthlyRate(), securityDeposit);
        leases.save(lease);
        paymentService.generateSchedule(lease);
        return lease;
    }

    /** Extends an active lease and bills the extra months. */
    public Lease renewLease(String leaseId, int additionalMonths) {
        Lease lease = getLease(leaseId);
        requireActive(lease);
        requireValidMonths(additionalMonths);
        LocalDate firstNewDue = lease.getEndDate().plusDays(1);
        lease.setEndDate(firstNewDue.plusMonths(additionalMonths).minusDays(1));
        leases.save(lease);
        paymentService.generateRentPayments(lease, firstNewDue, additionalMonths);
        return lease;
    }

    /** Ends a lease early: frees the bed and cancels unpaid rent after the effective date. */
    public Lease terminateLease(String leaseId, String reason, LocalDate effectiveDate) {
        Lease lease = getLease(leaseId);
        requireActive(lease);
        String cleanReason = Validator.requireNotBlank(reason, "Reason");
        LocalDate effective = effectiveDate == null ? LocalDate.now() : effectiveDate;
        lease.terminate(effective, cleanReason);
        leases.save(lease);
        roomService.releaseOccupant(lease.getRoomId(), lease.getTenantId());
        paymentService.cancelFutureDues(leaseId, effective);
        return lease;
    }

    /** Marks a lease as completed normally and frees the bed. */
    public Lease endLease(String leaseId) {
        Lease lease = getLease(leaseId);
        requireActive(lease);
        lease.end();
        leases.save(lease);
        roomService.releaseOccupant(lease.getRoomId(), lease.getTenantId());
        return lease;
    }

    /** Automatically ends every active lease whose last day has passed. Returns how many were closed. */
    public int closeExpiredLeases(LocalDate today) {
        int closed = 0;
        for (Lease lease : leases.findAll(l -> l.isActive() && l.getEndDate().isBefore(today))) {
            lease.end();
            leases.save(lease);
            roomService.releaseOccupant(lease.getRoomId(), lease.getTenantId());
            closed++;
        }
        return closed;
    }

    public Lease getLease(String id) {
        return leases.findById(id).orElseThrow(() -> new EntityNotFoundException("Lease", id));
    }

    public List<Lease> listAll() {
        return leases.findAll();
    }

    public List<Lease> listActive() {
        return leases.findAll(Lease::isActive);
    }

    public List<Lease> listByTenant(String tenantId) {
        return leases.findAll(l -> l.getTenantId().equals(tenantId));
    }

    public Optional<Lease> findActiveLeaseByTenant(String tenantId) {
        return leases.findAll(l -> l.isActive() && l.getTenantId().equals(tenantId)).stream().findFirst();
    }

    public List<Lease> findExpiringWithin(int days, LocalDate today) {
        LocalDate limit = today.plusDays(days);
        return leases.findAll(l -> l.isActive() && !l.getEndDate().isBefore(today) && !l.getEndDate().isAfter(limit));
    }

    private void requireActive(Lease lease) {
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new BusinessRuleException("Lease " + lease.getId() + " is " + lease.getStatus() + ", not active.");
        }
    }

    private void requireValidMonths(int months) {
        if (months < 1 || months > MAX_LEASE_MONTHS) {
            throw new ValidationException("Lease length must be between 1 and " + MAX_LEASE_MONTHS + " months.");
        }
    }
}
