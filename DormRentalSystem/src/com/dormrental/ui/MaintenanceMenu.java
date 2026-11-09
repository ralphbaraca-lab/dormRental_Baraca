package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.enums.MaintenancePriority;
import com.dormrental.enums.MaintenanceStatus;
import com.dormrental.model.MaintenanceRequest;
import com.dormrental.service.MaintenanceService;

public class MaintenanceMenu extends Menu {
    private final MaintenanceService service;

    public MaintenanceMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.service = ctx.getMaintenanceService();
    }

    @Override
    protected String title() {
        return "MAINTENANCE REQUESTS";
    }

    @Override
    protected String[] options() {
        return new String[]{
                "Report an issue",
                "List open requests (most urgent first)",
                "List all requests",
                "Assign to a staff member",
                "Update status",
                "Requests for one room"
        };
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                report();
                break;
            case 2:
                Display.print(tables.requests(service.listOpen()));
                break;
            case 3:
                Display.print(tables.requests(service.listAll()));
                break;
            case 4:
                assign();
                break;
            case 5:
                updateStatus();
                break;
            case 6:
                Display.print(tables.requests(service.listByRoom(in.readId("Room ID"))));
                break;
            default:
                break;
        }
    }

    private void report() {
        Display.print(tables.rooms(ctx.getRoomService().listRooms()));
        String roomId = in.readId("Room ID");
        String tenantId = in.readOptionalId("Reported by tenant ID (blank if reported by staff)");
        String title = in.readRequired("Short title");
        String description = in.readLine("Details (optional)");
        MaintenancePriority priority = in.readEnum("Priority", MaintenancePriority.class);
        MaintenanceRequest request = service.report(roomId, tenantId, title, description, priority);
        Display.success("Request " + request.getId() + " logged.");
    }

    private void assign() {
        Display.print(tables.requests(service.listOpen()));
        String id = in.readId("Request ID");
        String staff = in.readRequired("Assign to (staff name)");
        service.assign(id, staff);
        Display.success("Request " + id + " assigned to " + staff + ".");
    }

    private void updateStatus() {
        Display.print(tables.requests(service.listOpen()));
        String id = in.readId("Request ID");
        MaintenanceStatus status = in.readEnum("New status", MaintenanceStatus.class);
        String notes = in.readLine("Notes (optional)");
        service.updateStatus(id, status, notes);
        Display.success("Request " + id + " is now " + status + ".");
    }
}
