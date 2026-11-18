
package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.enums.MaintenancePriority;
import com.dormrental.enums.MaintenanceStatus;
import com.dormrental.model.MaintenanceRequest;
import com.dormrental.service.MaintenanceService;
import java.util.List;
import java.util.Objects;

public class MaintenanceMenu extends Menu {

    private final MaintenanceService service;

    private static final String[] OPTIONS = {
            "Report an issue",
            "List open requests (most urgent first)",
            "List all requests",
            "Assign to a staff member",
            "Update status",
            "Requests for one room"
    };

    public MaintenanceMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);

        this.service = Objects.requireNonNull(
                ctx.getMaintenanceService(),
                "MaintenanceService cannot be null."
        );
    }

    @Override
    protected String title() {
        return "MAINTENANCE REQUESTS";
    }

    @Override
    protected String[] options() {
        return OPTIONS.clone();
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1 -> report();
            case 2 -> listOpenRequests();
            case 3 -> listAllRequests();
            case 4 -> assign();
            case 5 -> updateStatus();
            case 6 -> listRoomRequests();
            default -> Display.error("Invalid menu selection.");
        }
    }

    // Reports a new maintenance issue.
    private void report() {
        Display.subheader("REPORT MAINTENANCE ISSUE");

        Display.print(tables.rooms(ctx.getRoomService().listRooms()));

        String roomId = in.readId("Room ID");

        String tenantId = in.readOptionalId(
                "Reported by tenant ID (blank if reported by staff)"
        );

        String title = in.readRequired("Short title");

        String description = in.readLine("Details (optional)");

        MaintenancePriority priority = in.readEnum(
                "Priority",
                MaintenancePriority.class
        );

        MaintenanceRequest request = service.report(
                roomId,
                tenantId,
                title,
                description,
                priority
        );

        Display.success(
                "Maintenance request " + request.getId() +
                " successfully reported."
        );
    }

    // Lists open maintenance requests.
    private void listOpenRequests() {
        Display.subheader("OPEN MAINTENANCE REQUESTS");

        List<MaintenanceRequest> requests = service.listOpen();

        displayRequests(
                requests,
                "There are no open maintenance requests."
        );
    }

    // Lists every maintenance request.
    private void listAllRequests() {
        Display.subheader("ALL MAINTENANCE REQUESTS");

        List<MaintenanceRequest> requests = service.listAll();

        displayRequests(
                requests,
                "No maintenance requests have been recorded."
        );
    }

    // Assigns a maintenance request to a staff member.
    private void assign() {
        Display.subheader("ASSIGN MAINTENANCE REQUEST");

        List<MaintenanceRequest> requests = service.listOpen();

        if (!displayRequests(
                requests,
                "There are no open requests available for assignment."
        )) {
            return;
        }

        String id = in.readId("Request ID");

        String staffName = in.readRequired("Assign to (staff name)");

        if (!in.confirm(
                "Assign request " + id + " to " + staffName + "?"
        )) {
            Display.info("Assignment cancelled.");
            return;
        }

        service.assign(id, staffName);

        Display.success(
                "Request " + id + " successfully assigned to " + staffName + "."
        );
    }

    // Updates the status of an existing maintenance request.
    private void updateStatus() {
        Display.subheader("UPDATE MAINTENANCE STATUS");

        List<MaintenanceRequest> requests = service.listOpen();

        if (!displayRequests(
                requests,
                "There are no open requests available for updating."
        )) {
            return;
        }

        String id = in.readId("Request ID");

        MaintenanceStatus status = in.readEnum(
                "New status",
                MaintenanceStatus.class
        );

        String notes = in.readLine("Notes (optional)");

        if (!in.confirm(
                "Update request " + id + " to " + status + "?"
        )) {
            Display.info("Status update cancelled.");
            return;
        }

        service.updateStatus(id, status, notes);

        Display.success(
                "Request " + id + " is now " + status + "."
        );
    }

    // Displays requests associated with a particular room.
    private void listRoomRequests() {
        Display.subheader("ROOM MAINTENANCE HISTORY");

        String roomId = in.readId("Room ID");

        List<MaintenanceRequest> requests = service.listByRoom(roomId);

        displayRequests(
                requests,
                "No maintenance requests found for room " + roomId + "."
        );
    }

    // Reusable method for displaying maintenance requests.
    private boolean displayRequests(
            List<MaintenanceRequest> requests,
            String emptyMessage
    ) {
        if (requests == null || requests.isEmpty()) {
            Display.info(emptyMessage);
            return false;
        }

        Display.print(tables.requests(requests));
        return true;
    }
}
