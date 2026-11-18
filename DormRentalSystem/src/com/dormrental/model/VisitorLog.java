
package com.dormrental.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a visitor's check-in and check-out record
 * at the dormitory front desk.
 */
public class VisitorLog implements Identifiable, Serializable {

    // Keep this value unchanged for serialization compatibility.
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String tenantId;
    private final String visitorName;
    private final String purpose;
    private final LocalDateTime timeIn;

    private LocalDateTime timeOut;

    // Constructor
    public VisitorLog(String id, String tenantId,
                      String visitorName, String purpose) {

        this.id = validateText(id, "Visitor log ID");
        this.tenantId = validateText(tenantId, "Tenant ID");
        this.visitorName = validateText(visitorName, "Visitor name");
        this.purpose = validateText(purpose, "Visit purpose");

        this.timeIn = LocalDateTime.now();
    }

    // Validates required text fields
    private static String validateText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null.");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " cannot be empty."
            );
        }

        return value.trim();
    }

    // Returns visitor log ID
    @Override
    public String getId() {
        return id;
    }

    // Returns tenant ID
    public String getTenantId() {
        return tenantId;
    }

    // Returns visitor name
    public String getVisitorName() {
        return visitorName;
    }

    // Returns visit purpose
    public String getPurpose() {
        return purpose;
    }

    // Returns check-in time
    public LocalDateTime getTimeIn() {
        return timeIn;
    }

    // Returns check-out time
    public LocalDateTime getTimeOut() {
        return timeOut;
    }

    // Checks whether the visitor is still inside
    public boolean isInside() {
        return timeOut == null;
    }

    /**
     * Records the visitor's departure.
     * A visitor can only check out once.
     */
    public void checkOut() {
        if (!isInside()) {
            throw new IllegalStateException(
                "Visitor " + visitorName + " has already checked out."
            );
        }

        this.timeOut = LocalDateTime.now();
    }

    /**
     * Returns the total visit duration.
     * If the visitor is still inside, calculates duration
     * from check-in until the current time.
     */
    public Duration getDuration() {
        LocalDateTime endTime = isInside()
                ? LocalDateTime.now()
                : timeOut;

        return Duration.between(timeIn, endTime);
    }

    // Returns formatted visit information
    @Override
    public String toString() {
        return "VisitorLog {" +
                "ID='" + id + '\'' +
                ", Visitor='" + visitorName + '\'' +
                ", Tenant ID='" + tenantId + '\'' +
                ", Purpose='" + purpose + '\'' +
                ", Time In=" + timeIn +
                ", Time Out=" + (isInside() ? "Still inside" : timeOut) +
                ", Status=" + (isInside() ? "Inside" : "Checked out") +
                '}';
    }
}
