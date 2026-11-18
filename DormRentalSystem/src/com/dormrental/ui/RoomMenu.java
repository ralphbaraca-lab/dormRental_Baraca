package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.enums.Amenity;
import com.dormrental.enums.RoomType;
import com.dormrental.model.Room;
import com.dormrental.service.RoomService;
import com.dormrental.util.MoneyUtil;
import java.math.BigDecimal;
import java.util.Set;

public class RoomMenu extends Menu {
    private final RoomService service;

    public RoomMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.service = ctx.getRoomService();
    }

    @Override
    protected String title() {
        return "ROOMS";
    }

    @Override
    protected String[] options() {
        return new String[]{
                "Add room",
                "List all rooms",
                "List rooms in a dormitory",
                "Search available rooms",
                "View room details",
                "Update rate / amenities",
                "Toggle maintenance mode",
                "Delete room"
        };
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                add();
                break;
            case 2:
                Display.print(tables.rooms(service.listRooms()));
                break;
            case 3:
                listByDormitory();
                break;
            case 4:
                search();
                break;
            case 5:
                details();
                break;
            case 6:
                update();
                break;
            case 7:
                toggleMaintenance();
                break;
            case 8:
                delete();
                break;
            default:
                break;
        }
    }

    private void add() {
        Display.print(tables.dormitories(ctx.getDormitoryService().listDormitories()));
        String dormitoryId = in.readId("Dormitory ID");
        String number = in.readRequired("Room number (e.g. 101)");
        int floor = in.readInt("Floor", 1, 100);
        RoomType type = in.readEnum("Room type", RoomType.class);
        BigDecimal rate = in.readMoney("Monthly rate per bed (PHP)");
        Set<Amenity> amenities = in.readEnumSet("Amenities", Amenity.class);
        Room room = service.addRoom(dormitoryId, number, floor, type, rate, amenities);
        Display.success("Room created with ID " + room.getId() + ".");
    }

    private void listByDormitory() {
        Display.print(tables.dormitories(ctx.getDormitoryService().listDormitories()));
        String dormitoryId = in.readId("Dormitory ID");
        ctx.getDormitoryService().getDormitory(dormitoryId); // throws if it doesn't exist, rather than showing an empty list
        Display.print(tables.rooms(service.listByDormitory(dormitoryId)));
    }

    private void search() {
        Display.info("Leave a filter blank to skip it.");
        String dormitoryId = in.readOptionalId("Dormitory ID");
        RoomType type = in.confirm("Filter by room type?") ? in.readEnum("Room type", RoomType.class) : null;
        BigDecimal maxRate = in.readOptionalMoney("Maximum rate per bed (PHP)");
        Display.print(tables.rooms(service.searchAvailable(dormitoryId, type, maxRate)));
    }

    private void details() {
        Room room = readRoom("Room ID");
        Display.subheader("Room " + room.getRoomNumber() + " - " + tables.dormitoryName(room.getDormitoryId()));
        field("ID", room.getId());
        field("Floor", room.getFloor());
        field("Type", room.getType());
        field("Rate / bed", MoneyUtil.format(room.getMonthlyRate()));
        field("Status", room.getStatus() + " (" + room.getOccupancy() + "/" + room.getCapacity() + " beds taken)");
        field("Amenities", room.getAmenities().isEmpty() ? "none" : room.getAmenities().toString());
        Display.info("  Occupants  :");
        if (room.getOccupantIds().isEmpty()) {
            Display.info("    (none)");
        }
        for (String tenantId : room.getOccupantIds()) {
            Display.info("    " + tenantId + "  " + tables.tenantName(tenantId));
        }
    }

    private void update() {
        Display.print(tables.rooms(service.listRooms()));
        // Looking the room up now means a wrong id is reported before any of the questions below.
        String id = readRoom("Room ID to update").getId();
        Display.info("Leave a field blank to keep its current value. Existing leases keep their signed rate.");

        BigDecimal rate = readOptionalRate("New monthly rate per bed (PHP)");
        Set<Amenity> amenities = in.confirm("Replace the amenity list?") ? in.readEnumSet("Amenities", Amenity.class) : null;
        if (rate == null && amenities == null) {
            Display.info("Nothing to change.");
            return;
        }
        service.updateRoom(id, rate, amenities);
        Display.success("Room " + id + " updated.");
    }

    private void toggleMaintenance() {
        Room room = readRoom("Room ID");
        boolean target = !room.isUnderMaintenance();
        String question = target ? "Put room " + room.getRoomNumber() + " under maintenance (no new tenants)?"
                : "Return room " + room.getRoomNumber() + " to service?";
        if (in.confirm(question)) {
            service.setUnderMaintenance(room.getId(), target);
            Display.success("Room " + room.getRoomNumber() + " is now " + (target ? "under maintenance." : "available again."));
        } else {
            Display.info("Cancelled.");
        }
    }

    private void delete() {
        Room room = readRoom("Room ID to delete");
        // RoomService enforces this too; checking here just avoids asking for a confirmation that can only fail.
        if (room.getOccupancy() > 0) {
            Display.error("Room " + room.getRoomNumber() + " still has tenants living in it.");
            return;
        }
        if (in.confirm("Delete room " + room.getRoomNumber() + " of " + tables.dormitoryName(room.getDormitoryId()) + "?")) {
            service.deleteRoom(room.getId());
            Display.success("Room deleted.");
        } else {
            Display.info("Cancelled.");
        }
    }

    // ---- helpers -------------------------------------------------------------------------

    /** Asks for a room id and returns that room (throws if there is no such room). */
    private Room readRoom(String prompt) {
        return service.getRoom(in.readId(prompt));
    }

    /** A new rate, or null if left blank. Zero is refused here, instead of after the other questions have been answered. */
    private BigDecimal readOptionalRate(String prompt) {
        while (true) {
            BigDecimal rate = in.readOptionalMoney(prompt);
            if (rate == null || rate.signum() > 0) {
                return rate;
            }
            Display.error("The rate must be greater than zero, or leave it blank to keep the current one.");
        }
    }

    /** One aligned "  Label      : value" line in the details view. */
    private static void field(String label, Object value) {
        Display.info(String.format("  %-11s: %s", label, value));
    }
}