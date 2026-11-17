package com.dormrental.service;

import com.dormrental.enums.Gender;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.exception.DuplicateEntityException;
import com.dormrental.exception.EntityNotFoundException;
import com.dormrental.exception.ValidationException;
import com.dormrental.model.Lease;
import com.dormrental.model.Tenant;
import com.dormrental.repository.Repository;
import com.dormrental.util.Validator;
import java.util.List;
import java.util.Locale;

public class TenantService {
    private final Repository<Tenant> tenants;
    private final Repository<Lease> leases;

    public TenantService(Repository<Tenant> tenants, Repository<Lease> leases) {
        this.tenants = tenants;
        this.leases = leases;
    }

    public Tenant registerTenant(String firstName, String lastName, String email, String phone,
                                 Gender gender, String school, String homeAddress,
                                 String guardianName, String guardianPhone) {
        String first = Validator.requireNotBlank(firstName, "First name");
        String last = Validator.requireNotBlank(lastName, "Last name");
        String cleanEmail = Validator.requireEmail(email);
        String cleanPhone = Validator.requirePhone(phone);
        if (gender == null) {
            throw new ValidationException("Gender is required.");
        }
        String cleanGuardianPhone = Validator.isBlank(guardianPhone) ? "" : Validator.requirePhone(guardianPhone);
        if (!tenants.findAll(t -> t.getEmail().equalsIgnoreCase(cleanEmail)).isEmpty()) {
            throw new DuplicateEntityException("A tenant with email " + cleanEmail + " is already registered.");
        }
        Tenant tenant = new Tenant(tenants.nextId(), first, last, cleanEmail, cleanPhone, gender,
                orEmpty(school), orEmpty(homeAddress), orEmpty(guardianName), cleanGuardianPhone);
        return tenants.save(tenant);
    }

    /** Blank arguments mean "keep the current value". */
    public Tenant updateContact(String id, String email, String phone, String homeAddress) {
        Tenant tenant = getTenant(id);
        if (!Validator.isBlank(email)) {
            String cleanEmail = Validator.requireEmail(email);
            boolean taken = !tenants.findAll(t -> t.getEmail().equalsIgnoreCase(cleanEmail) && !t.getId().equals(id)).isEmpty();
            if (taken) {
                throw new DuplicateEntityException("Email " + cleanEmail + " belongs to another tenant.");
            }
            tenant.setEmail(cleanEmail);
        }
        if (!Validator.isBlank(phone)) {
            tenant.setPhone(Validator.requirePhone(phone));
        }
        if (!Validator.isBlank(homeAddress)) {
            tenant.setHomeAddress(homeAddress.trim());
        }
        return tenants.save(tenant);
    }

    public Tenant getTenant(String id) {
        return tenants.findById(id).orElseThrow(() -> new EntityNotFoundException("Tenant", id));
    }

    /** The tenant's full name, or the id itself if no such tenant exists. For display only. */
    public String getNameOrId(String id) {
        return tenants.findById(id).map(Tenant::getFullName).orElse(id);
    }

    public List<Tenant> listTenants() {
        return tenants.findAll();
    }

    public List<Tenant> listActive() {
        return tenants.findAll(Tenant::isActive);
    }

    public List<Tenant> searchByName(String keyword) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return tenants.findAll(t -> t.getFullName().toLowerCase(Locale.ROOT).contains(needle));
    }

    public Tenant deactivate(String id) {
        Tenant tenant = getTenant(id);
        boolean hasActiveLease = !leases.findAll(l -> l.isActive() && l.getTenantId().equals(id)).isEmpty();
        if (hasActiveLease) {
            throw new BusinessRuleException(tenant.getFullName() + " still has an active lease. End or terminate it first.");
        }
        tenant.setActive(false);
        return tenants.save(tenant);
    }

    public Tenant reactivate(String id) {
        Tenant tenant = getTenant(id);
        tenant.setActive(true);
        return tenants.save(tenant);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}