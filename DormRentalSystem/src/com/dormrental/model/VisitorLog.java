package com.dormrental.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** A visitor sign-in/sign-out record at the front desk. */
public class VisitorLog implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String tenantId;
    private final String visitorName;
    private final String purpose;
    private final LocalDateTime timeIn;
    private LocalDateTime timeOut;

    public VisitorLog(String id, String tenantId, String visitorName, String purpose) {
        this.id = id;
        this.tenantId = tenantId;
        this.visitorName = visitorName;
        this.purpose = purpose;
        this.timeIn = LocalDateTime.now();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public String getPurpose() {
        return purpose;
    }

    public LocalDateTime getTimeIn() {
        return timeIn;
    }

    public LocalDateTime getTimeOut() {
        return timeOut;
    }

    public boolean isInside() {
        return timeOut == null;
    }

    public void checkOut() {
        this.timeOut = LocalDateTime.now();
    }
}
