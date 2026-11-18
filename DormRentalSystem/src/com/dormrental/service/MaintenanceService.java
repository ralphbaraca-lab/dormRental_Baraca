
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
import java.util.Objects;

public class MaintenanceService {

    private static final String NOTE_SEPARATOR = " | ";

    /**
     * Sorts requests by priority (highest first),
     * then by report date (oldest first).
     */
    private static final Comparator<MaintenanceRequest> URGENT_FIRST =
            Comparator.comparing(MaintenanceRequest::getPriority)
                    .reversed()
                    .thenComparing(MaintenanceRequest::getReportedOn);

    private final Repository<MaintenanceRequest> requests;
    private final RoomService roomService;
    private final TenantService tenantService;

    public MaintenanceService(
            Repository<MaintenanceRequest> requests,
            RoomService roomService,
            TenantService tenantService
    ) {
        this.requests = Objects.requireNonNull(
                requests,
                "MaintenanceRequest repository cannot be null."
        );

        this.roomService = Objects.requireNonNull(
                roomService,
                "RoomService cannot be null."
        );

        this.tenantService = Objects.requireNonNull(
                tenantService,
                "TenantService cannot be null."
        );
    }

    /**
     * Creates a new maintenance request.
     * Tenant ID may be blank when reported by staff.
     */
    public MaintenanceRequest report(
            String roomId,
            String tenantId,
            String title,
            String description,
            MaintenancePriority priority
    ) {
        String cleanRoomId = Validator.requireNotBlank(roomId, "Room ID");
        String cleanTitle = Validator.requireNotBlank(title, "Title");

        String cleanDescription = description == null
                ? ""
                : description.trim();

        MaintenancePriority cleanPriority = priority == null
                ? MaintenancePriority.MEDIUM
                : priority;

        // Validate that the room exists.
        roomService.getRoom(cleanRoomId);

        // Validate reporter if a tenant ID was provided.
        String reporterId = null;

        if (!Validator.isBlank(tenantId)) {
            reporterId = tenantService.getTenant(tenantId.trim()).getId();
        }

        MaintenanceRequest request = new MaintenanceRequest(
                requests.nextId(),
                cleanRoomId,
                reporterId,
                cleanTitle,
                cleanDescription,
                cleanPriority
        );

        return requests.save(request);
    }

    /**
     * Assigns a maintenance request to a staff member.
     * Automatically changes OPEN requests to IN_PROGRESS.
     */
    public MaintenanceRequest assign(
            String requestId,
            String staffName
    ) {
        String cleanStaffName = Validator.requireNotBlank(
                staffName,
                "Staff name"
        );

        MaintenanceRequest request = get(requestId);

        if (request.getStatus() == MaintenanceStatus.RESOLVED) {
            throw new ValidationException(
                    "Cannot assign a resolved maintenance request."
            );
        }

        request.setAssignedTo(cleanStaffName);

        if (request.getStatus() == MaintenanceStatus.OPEN) {
            request.setStatus(MaintenanceStatus.IN_PROGRESS);
        }

        return requests.save(request);
    }

    /**
     * Updates the status of a maintenance request
     * and optionally appends notes.
     */
    public MaintenanceRequest updateStatus(
            String requestId,
            MaintenanceStatus status,
            String notes
    ) {
        if (status == null) {
            throw new ValidationException(
                    "Maintenance status is required."
            );
        }

        MaintenanceRequest request = get(requestId);

        MaintenanceStatus currentStatus = request.getStatus();

        validateStatusTransition(currentStatus, status);

        updateResolutionDate(request, currentStatus, status);

        appendNotes(request, notes);

        request.setStatus(status);

        return requests.save(request);
    }

    /**
     * Checks whether a status transition is allowed.
     */
    private void validateStatusTransition(
            MaintenanceStatus current,
            MaintenanceStatus next
    ) {
        if (current == next) {
            return;
        }

        if (current == MaintenanceStatus.RESOLVED) {
            throw new ValidationException(
                    "A resolved request cannot be reopened through a status update."
            );
        }
    }

    /**
     * Maintains the resolution date when status changes.
     */
    private void updateResolutionDate(
            MaintenanceRequest request,
            MaintenanceStatus current,
            MaintenanceStatus next
    ) {
        if (next == MaintenanceStatus.RESOLVED) {

            if (current != MaintenanceStatus.RESOLVED) {
                request.setResolvedOn(LocalDate.now());
            }

        } else {
            request.setResolvedOn(null);
        }
    }

    /**
     * Appends new notes without replacing existing notes.
     */
    private void appendNotes(
            MaintenanceRequest request,
            String notes
    ) {
        if (Validator.isBlank(notes)) {
            return;
        }

        String cleanNotes = notes.trim();
        String existingNotes = request.getNotes();

        if (Validator.isBlank(existingNotes)) {
            request.setNotes(cleanNotes);
        } else {
            request.setNotes(
                    existingNotes.trim() +
                    NOTE_SEPARATOR +
                    cleanNotes
            );
        }
    }

    /**
     * Finds a maintenance request by ID.
     */
    public MaintenanceRequest get(String id) {
        String cleanId = Validator.requireNotBlank(id, "Request ID");

        return requests.findById(cleanId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Maintenance request",
                                cleanId
                        )
                );
    }

    /**
     * Returns all maintenance requests, most urgent first.
     */
    public List<MaintenanceRequest> listAll() {
        return urgentFirst(requests.findAll());
    }

    /**
     * Returns open maintenance requests, most urgent first.
     */
    public List<MaintenanceRequest> listOpen() {
        return urgentFirst(
                requests.findAll(MaintenanceRequest::isOpen)
        );
    }

    /**
     * Returns maintenance requests belonging to a room.
     */
    public List<MaintenanceRequest> listByRoom(String roomId) {
        String cleanRoomId = Validator.requireNotBlank(
                roomId,
                "Room ID"
        );

        roomService.getRoom(cleanRoomId);

        return urgentFirst(
                requests.findAll(
                        request -> cleanRoomId.equals(request.getRoomId())
                )
        );
    }

    /**
     * Sorts a copy instead of modifying the repository's original list.
     */
    private static List<MaintenanceRequest> urgentFirst(
            List<MaintenanceRequest> list
    ) {
        List<MaintenanceRequest> sorted = new ArrayList<>(list);

        sorted.sort(URGENT_FIRST);

        return sorted;
    }
}
