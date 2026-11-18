package com.dormrental.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/** A visitor sign-in/sign-out record at the front desk. */
public class VisitorLog implements Identifiable, Serializable {
    // Keep this value: changing it (or the fields below) makes records already saved to disk unreadable.
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String tenantId;
    private final String visitorName;
    private final String purpose;
    private final LocalDateTime timeIn;
    private LocalDateTime timeOut;

    public VisitorLog(String id, String tenantId, String visitorName, String purpose) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.visitorName = Objects.requireNonNull(visitorName, "visitorName");
        this.purpose = Objects.requireNonNull(purpose, "purpose");
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

    /** When the visitor left, or null if they are still inside. */
    public LocalDateTime getTimeOut() {
        return timeOut;
    }

    public boolean isInside() {
        return timeOut == null;
    }

    /** Signs the visitor out now. A visitor can only be signed out once, so the real time out is never overwritten. */
    public void checkOut() {
        if (!isInside()) {
            throw new IllegalStateException("Visitor " + visitorName + " has already checked out.");
        }
        this.timeOut = LocalDateTime.now();
    }

    /** How long the visit lasted, or how long the visitor has been inside so far if they haven't left. */
    public Duration getDuration() {
        LocalDateTime end = isInside() ? LocalDateTime.now() : timeOut;
        return Duration.between(timeIn, end);
    }

    @Override
    public String toString() {
        return "VisitorLog{id=" + id + ", visitor=" + visitorName + ", tenant=" + tenantId
                + ", in=" + timeIn + ", out=" + (isInside() ? "(inside)" : timeOut) + "}";
    }
}