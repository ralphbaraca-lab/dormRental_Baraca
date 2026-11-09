package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.enums.DormType;
import com.dormrental.model.Dormitory;
import com.dormrental.service.DormitoryService;

public class DormitoryMenu extends Menu {
    private final DormitoryService service;

    public DormitoryMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.service = ctx.getDormitoryService();
    }

    @Override
    protected String title() {
        return "DORMITORIES";
    }

    @Override
    protected String[] options() {
        return new String[]{"Add dormitory", "List dormitories", "Update dormitory", "Delete dormitory"};
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                add();
                break;
            case 2:
                Display.print(tables.dormitories(service.listDormitories()));
                break;
            case 3:
                update();
                break;
            case 4:
                delete();
                break;
            default:
                break;
        }
    }

    private void add() {
        String name = in.readRequired("Dormitory name");
        String address = in.readRequired("Address");
        DormType type = in.readEnum("Dormitory type", DormType.class);
        String contact = in.readLine("Contact number (optional)");
        Dormitory dormitory = service.addDormitory(name, address, type, contact);
        Display.success("Dormitory created with ID " + dormitory.getId() + ".");
    }

    private void update() {
        Display.print(tables.dormitories(service.listDormitories()));
        String id = in.readId("Dormitory ID to update");
        service.getDormitory(id); // fail early if the id is wrong
        Display.info("Leave a field blank to keep its current value.");
        String name = in.readLine("New name");
        String address = in.readLine("New address");
        String contact = in.readLine("New contact number");
        DormType type = in.confirm("Change the dormitory type?") ? in.readEnum("New type", DormType.class) : null;
        service.updateDormitory(id, name, address, type, contact);
        Display.success("Dormitory " + id + " updated.");
    }

    private void delete() {
        Display.print(tables.dormitories(service.listDormitories()));
        String id = in.readId("Dormitory ID to delete");
        Dormitory dormitory = service.getDormitory(id);
        if (in.confirm("Delete " + dormitory.getName() + "?")) {
            service.deleteDormitory(id);
            Display.success("Dormitory deleted.");
        } else {
            Display.info("Cancelled.");
        }
    }
}
