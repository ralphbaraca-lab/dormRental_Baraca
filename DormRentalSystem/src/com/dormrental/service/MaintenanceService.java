package com.dormrental.service;

import com.dormrental.enums.MaintenancePriority;
import com.dormrental.enums.MaintenanceStatus;
import com.dormrental.exception.EntityNotFoundException;
import com.dormrental.model.MaintenanceRequest;
import com.dormrental.repository.Repository;
import com.dormrental.util.Validator;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class MaintenanceService {
    /** Most urgent first, then oldest first. */
    private static final Comparator<MaintenanceRequest> URGENT_FIRST =
            Comparator.comparing(MaintenanceRequest::getPriority).reversed()
                    .thenComparing(MaintenanceRequest::getReportedOn);

    private final Repository<MaintenanceRequest> requests;
    private final RoomService roomService;
    private final TenantService tenantService;

    public MaintenanceService(Repository<MaintenanceRequest> requests, RoomService roomService, TenantService tenantService) {
        this.requests = requests;
        this.roomService = roomService;
        this.tenantService = tenantService;
    }

    /** {@code tenantId} may be blank when the issue is reported by staff. */
    public MaintenanceRequest report(String roomId, String tenantId, String title, String description,
                                     MaintenancePriority priority) {
        roomService.getRoom(roomId);
        String reporter = null;
        if (!Validator.isBlank(tenantId)) {
            reporter = tenantService.getTenant(tenantId).getId();
        }
        String cleanTitle = Validator.requireNotBlank(title, "Title");
        String cleanDescription = description == null ? "" : description.trim();
        MaintenancePriority level = priority == null ? MaintenancePriority.MEDIUM : priority;
        return requests.save(new MaintenanceRequest(requests.nextId(), roomId, reporter, cleanTitle, cleanDescription, level));
    }

    public MaintenanceRequest assign(String requestId, String staffName) {
        MaintenanceRequest request = get(requestId);
        request.setAssignedTo(Validator.requireNotBlank(staffName, "Staff name"));
        if (request.getStatus() == MaintenanceStatus.OPEN) {
            request.setStatus(MaintenanceStatus.IN_PROGRESS);
        }
        return requests.save(request);
    }

    public MaintenanceRequest updateStatus(String requestId, MaintenanceStatus status, String notes) {
        MaintenanceRequest request = get(requestId);
        request.setStatus(status);
        request.setResolvedOn(status == MaintenanceStatus.RESOLVED ? LocalDate.now() : null);
        if (!Validator.isBlank(notes)) {
            String existing = request.getNotes();
            request.setNotes(Validator.isBlank(existing) ? notes.trim() : existing + " | " + notes.trim());
        }
        return requests.save(request);
    }

    public MaintenanceRequest get(String id) {
        return requests.findById(id).orElseThrow(() -> new EntityNotFoundException("Maintenance request", id));
    }

    public List<MaintenanceRequest> listAll() {
        List<MaintenanceRequest> all = requests.findAll();
        all.sort(URGENT_FIRST);
        return all;
    }

    public List<MaintenanceRequest> listOpen() {
        List<MaintenanceRequest> open = requests.findAll(MaintenanceRequest::isOpen);
        open.sort(URGENT_FIRST);
        return open;
    }

    public List<MaintenanceRequest> listByRoom(String roomId) {
        List<MaintenanceRequest> result = requests.findAll(r -> r.getRoomId().equals(roomId));
        result.sort(URGENT_FIRST);
        return result;
    }
}
