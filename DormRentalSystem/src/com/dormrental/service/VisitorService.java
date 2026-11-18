package com.dormrental.service;

import com.dormrental.exception.BusinessRuleException;
import com.dormrental.exception.EntityNotFoundException;
import com.dormrental.model.Tenant;
import com.dormrental.model.VisitorLog;
import com.dormrental.repository.Repository;
import com.dormrental.util.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VisitorService {
    private static final String DEFAULT_PURPOSE = "Visit";

    /** Newest visit first; the id breaks ties so visits logged in the same instant keep a steady order. */
    private static final Comparator<VisitorLog> NEWEST_FIRST =
            Comparator.comparing(VisitorLog::getTimeIn).reversed()
                    .thenComparing(VisitorLog::getId, Comparator.reverseOrder());

    private final Repository<VisitorLog> logs;
    private final TenantService tenantService;

    public VisitorService(Repository<VisitorLog> logs, TenantService tenantService) {
        this.logs = logs;
        this.tenantService = tenantService;
    }

    public VisitorLog checkIn(String tenantId, String visitorName, String purpose) {
        // Check the input itself first, before looking anything up.
        String name = Validator.requireNotBlank(visitorName, "Visitor name");
        String reason = Validator.isBlank(purpose) ? DEFAULT_PURPOSE : purpose.trim();

        Tenant tenant = tenantService.getTenant(tenantId);
        if (!tenant.isActive()) {
            throw new BusinessRuleException(tenant.getFullName() + " is not an active tenant.");
        }
        return logs.save(new VisitorLog(logs.nextId(), tenantId, name, reason));
    }

    public VisitorLog checkOut(String logId) {
        VisitorLog log = get(logId);
        if (!log.isInside()) {
            throw new BusinessRuleException("Visitor " + log.getVisitorName() + " has already checked out.");
        }
        log.checkOut();
        return logs.save(log);
    }

    public VisitorLog get(String id) {
        return logs.findById(id).orElseThrow(() -> new EntityNotFoundException("Visitor log", id));
    }

    public List<VisitorLog> listInside() {
        return newestFirst(logs.findAll(VisitorLog::isInside));
    }

    public List<VisitorLog> listByTenant(String tenantId) {
        return newestFirst(logs.findAll(v -> v.getTenantId().equals(tenantId)));
    }

    public List<VisitorLog> listAll() {
        return newestFirst(logs.findAll());
    }

    /** Sorts a copy, so we never reorder (or fail on) a list the repository may be holding on to. */
    private static List<VisitorLog> newestFirst(List<VisitorLog> list) {
        List<VisitorLog> sorted = new ArrayList<>(list);
        sorted.sort(NEWEST_FIRST);
        return sorted;
    }
}