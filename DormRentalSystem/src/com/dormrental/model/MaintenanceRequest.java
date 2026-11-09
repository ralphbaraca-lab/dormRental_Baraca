package com.dormrental.model;

import com.dormrental.enums.MaintenancePriority;
import com.dormrental.enums.MaintenanceStatus;

import java.io.Serializable;
import java.time.LocalDate;

public class MaintenanceRequest implements Identifiable, Serializable {
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
        this.id = id;
        this.roomId = roomId;
        this.tenantId = tenantId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.reportedOn = LocalDate.now();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

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
        this.status = status;
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
}
