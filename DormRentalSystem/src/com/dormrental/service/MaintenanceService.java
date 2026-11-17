package com.dormrental.service;

import com.dormrental.enums.MaintenancePriority;
import com.dormrental.enums.MaintenanceStatus;
import com.dormrental.exception.EntityNotFoundException;
import com.dormrental.exception.ValidationException;
import com.dormrental.model.MaintenanceRequest;
import com.dormrental.repository.Repository;
import com.dormrental.util.Validator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MaintenanceService {
    private static final String NOTE_SEPARATOR = " | ";

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
        String cleanTitle = Validator.requireNotBlank(title, "Title");
        String cleanDescription = description == null ? "" : description.trim();
        MaintenancePriority level = priority == null ? MaintenancePriority.MEDIUM : priority;

        roomService.getRoom(roomId); // throws if the room doesn't exist
        String reporter = Validator.isBlank(tenantId) ? null : tenantService.getTenant(tenantId).getId();

        return requests.save(new MaintenanceRequest(requests.nextId(), roomId, reporter, cleanTitle, cleanDescription, level));
    }

    public MaintenanceRequest assign(String requestId, String staffName) {
        String cleanStaffName = Validator.requireNotBlank(staffName, "Staff name");
        MaintenanceRequest request = get(requestId);
        request.setAssignedTo(cleanStaffName);
        if (request.getStatus() == MaintenanceStatus.OPEN) {
            request.setStatus(MaintenanceStatus.IN_PROGRESS);
        }
        return requests.save(request);
    }

    public MaintenanceRequest updateStatus(String requestId, MaintenanceStatus status, String notes) {
        if (status == null) {
            throw new ValidationException("Status is required.");
        }
        MaintenanceRequest request = get(requestId);
        boolean wasResolved = request.getStatus() == MaintenanceStatus.RESOLVED;

        request.setStatus(status);
        if (status != MaintenanceStatus.RESOLVED) {
            request.setResolvedOn(null);
        } else if (!wasResolved) {
            // Only stamp the date when it actually becomes resolved, so adding a note
            // to an already-resolved request doesn't overwrite the original date.
            request.setResolvedOn(LocalDate.now());
        }

        if (!Validator.isBlank(notes)) {
            String existing = request.getNotes();
            request.setNotes(Validator.isBlank(existing) ? notes.trim() : existing + NOTE_SEPARATOR + notes.trim());
        }
        return requests.save(request);
    }

    public MaintenanceRequest get(String id) {
        return requests.findById(id).orElseThrow(() -> new EntityNotFoundException("Maintenance request", id));
    }

    public List<MaintenanceRequest> listAll() {
        return urgentFirst(requests.findAll());
    }

    public List<MaintenanceRequest> listOpen() {
        return urgentFirst(requests.findAll(MaintenanceRequest::isOpen));
    }

    public List<MaintenanceRequest> listByRoom(String roomId) {
        return urgentFirst(requests.findAll(r -> r.getRoomId().equals(roomId)));
    }

    /** Sorts a copy, so we never reorder (or fail on) a list the repository may be holding on to. */
    private static List<MaintenanceRequest> urgentFirst(List<MaintenanceRequest> list) {
        List<MaintenanceRequest> sorted = new ArrayList<>(list);
        sorted.sort(URGENT_FIRST);
        return sorted;
    }
}