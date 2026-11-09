package com.dormrental.service;

import com.dormrental.exception.BusinessRuleException;
import com.dormrental.exception.EntityNotFoundException;
import com.dormrental.model.Tenant;
import com.dormrental.model.VisitorLog;
import com.dormrental.repository.Repository;
import com.dormrental.util.Validator;

import java.util.Comparator;
import java.util.List;

public class VisitorService {
    private static final Comparator<VisitorLog> NEWEST_FIRST = Comparator.comparing(VisitorLog::getTimeIn).reversed();

    private final Repository<VisitorLog> logs;
    private final TenantService tenantService;

    public VisitorService(Repository<VisitorLog> logs, TenantService tenantService) {
        this.logs = logs;
        this.tenantService = tenantService;
    }

    public VisitorLog checkIn(String tenantId, String visitorName, String purpose) {
        Tenant tenant = tenantService.getTenant(tenantId);
        if (!tenant.isActive()) {
            throw new BusinessRuleException(tenant.getFullName() + " is not an active tenant.");
        }
        String name = Validator.requireNotBlank(visitorName, "Visitor name");
        String reason = purpose == null || purpose.trim().isEmpty() ? "Visit" : purpose.trim();
        return logs.save(new VisitorLog(logs.nextId(), tenantId, name, reason));
    }

    public VisitorLog checkOut(String logId) {
        VisitorLog log = logs.findById(logId).orElseThrow(() -> new EntityNotFoundException("Visitor log", logId));
        if (!log.isInside()) {
            throw new BusinessRuleException("Visitor " + log.getVisitorName() + " has already checked out.");
        }
        log.checkOut();
        return logs.save(log);
    }

    public List<VisitorLog> listInside() {
        List<VisitorLog> result = logs.findAll(VisitorLog::isInside);
        result.sort(NEWEST_FIRST);
        return result;
    }

    public List<VisitorLog> listByTenant(String tenantId) {
        List<VisitorLog> result = logs.findAll(v -> v.getTenantId().equals(tenantId));
        result.sort(NEWEST_FIRST);
        return result;
    }

    public List<VisitorLog> listAll() {
        List<VisitorLog> result = logs.findAll();
        result.sort(NEWEST_FIRST);
        return result;
    }
}
