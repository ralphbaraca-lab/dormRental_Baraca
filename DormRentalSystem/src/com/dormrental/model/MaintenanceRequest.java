package com.dormrental.model;

import com.dormrental.enums.MaintenancePriority;
import com.dormrental.enums.MaintenanceStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class MaintenanceRequest implements Identifiable, Serializable {
    // Keep this value: changing it (or the fields below) makes requests already saved to disk unreadable.
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String roomId;
    private final String tenantId; // who reported it; may be null when staff report it
    private final String title;
    private final String description;
    private final MaintenancePriority priority;
    private MaintenanceStatus status = MaintenanceStatus.OPEN;
    private final LocalDate reportedOn;
    private LocalDate resolvedOn;
    private String assignedTo;
    private String notes;

    public MaintenanceRequest(String id, String roomId, String tenantId, String title,
                              String description, MaintenancePriority priority) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.roomId = Objects.requireNonNull(roomId, "roomId must not be null");
        this.tenantId = tenantId; // the only optional one
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.reportedOn = LocalDate.now();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    /** The tenant who reported it, or null when staff reported it. */
    public String getTenantId() {
        return tenantId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public MaintenancePriority getPriority() {
        return priority;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public LocalDate getReportedOn() {
        return reportedOn;
    }

    public LocalDate getResolvedOn() {
        return resolvedOn;
    }

    public void setResolvedOn(LocalDate resolvedOn) {
        this.resolvedOn = resolvedOn;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isOpen() {
        return status != MaintenanceStatus.RESOLVED;
    }

    @Override
    public String toString() {
        return "MaintenanceRequest{" +
                "id=" + id +
                ", roomId=" + roomId +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", reportedOn=" + reportedOn +
                ", resolvedOn=" + resolvedOn +
                ", assignedTo=" + assignedTo +
                '}';
    }
}