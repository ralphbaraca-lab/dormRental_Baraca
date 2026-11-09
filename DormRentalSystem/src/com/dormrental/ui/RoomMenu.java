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
        ctx.getDormitoryService().getDormitory(dormitoryId);
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
        String id = in.readId("Room ID");
        Room room = service.getRoom(id);
        Display.subheader("Room " + room.getRoomNumber() + " - " + tables.dormitoryName(room.getDormitoryId()));
        Display.info("  ID         : " + room.getId());
        Display.info("  Floor      : " + room.getFloor());
        Display.info("  Type       : " + room.getType());
        Display.info("  Rate / bed : " + MoneyUtil.format(room.getMonthlyRate()));
        Display.info("  Status     : " + room.getStatus() + " (" + room.getOccupancy() + "/" + room.getCapacity() + " beds taken)");
        Display.info("  Amenities  : " + (room.getAmenities().isEmpty() ? "none" : room.getAmenities().toString()));
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
        String id = in.readId("Room ID to update");
        service.getRoom(id);
        Display.info("Leave a field blank to keep its current value. Existing leases keep their signed rate.");
        BigDecimal rate = in.readOptionalMoney("New monthly rate per bed (PHP)");
        Set<Amenity> amenities = in.confirm("Replace the amenity list?") ? in.readEnumSet("Amenities", Amenity.class) : null;
        service.updateRoom(id, rate, amenities);
        Display.success("Room " + id + " updated.");
    }

    private void toggleMaintenance() {
        String id = in.readId("Room ID");
        Room room = service.getRoom(id);
        boolean target = !room.isUnderMaintenance();
        String question = target ? "Put room " + room.getRoomNumber() + " under maintenance (no new tenants)?"
                : "Return room " + room.getRoomNumber() + " to service?";
        if (in.confirm(question)) {
            service.setUnderMaintenance(id, target);
            Display.success("Room " + room.getRoomNumber() + " is now " + (target ? "under maintenance." : "available again."));
        } else {
            Display.info("Cancelled.");
        }
    }

    private void delete() {
        String id = in.readId("Room ID to delete");
        Room room = service.getRoom(id);
        if (in.confirm("Delete room " + room.getRoomNumber() + " of " + tables.dormitoryName(room.getDormitoryId()) + "?")) {
            service.deleteRoom(id);
            Display.success("Room deleted.");
        } else {
            Display.info("Cancelled.");
        }
    }
}
