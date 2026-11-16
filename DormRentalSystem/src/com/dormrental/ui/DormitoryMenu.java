package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.enums.DormType;
import com.dormrental.model.Dormitory;
import com.dormrental.service.DormitoryService;
import java.util.List;

public class DormitoryMenu extends Menu {
    private record MenuEntry(String label, Runnable action) {}

    private final DormitoryService service;
    private final List<MenuEntry> entries;

    public DormitoryMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.service = ctx.getDormitoryService();
        this.entries = List.of(
                new MenuEntry("Add dormitory", this::add),
                new MenuEntry("List dormitories", this::list),
                new MenuEntry("Update dormitory", this::update),
                new MenuEntry("Delete dormitory", this::delete)
        );
    }

    @Override
    protected String title() {
        return "DORMITORIES";
    }

    @Override
    protected String[] options() {
        return entries.stream()
                .map(MenuEntry::label)
                .toArray(String[]::new);
    }

    @Override
    protected void handle(int choice) {
        int index = choice - 1;
        if (index < 0 || index >= entries.size()) {
            Display.error("Invalid menu selection.");
            return;
        }
        entries.get(index).action().run();
    }

    private void list() {
        List<Dormitory> dormitories = service.listDormitories();
        if (dormitories == null || dormitories.isEmpty()) {
            Display.info("No dormitories found.");
            return;
        }
        Display.print(tables.dormitories(dormitories));
    }

    private void add() {
        String name = in.readRequired("Dormitory name");
        String address = in.readRequired("Address");
        DormType type = in.readEnum("Dormitory type", DormType.class);
        String contact = blankToNull(in.readLine("Contact number (optional)"));

        Dormitory dormitory = service.addDormitory(name, address, type, contact);
        Display.success("Dormitory created with ID " + dormitory.getId() + ".");
    }

    private void update() {
        List<Dormitory> dormitories = service.listDormitories();
        if (dormitories == null || dormitories.isEmpty()) {
            Display.info("No dormitories available to update.");
            return;
        }

        Display.print(tables.dormitories(dormitories));
        String id = in.readId("Dormitory ID to update");
        Dormitory existing = service.getDormitory(id);
        if (existing == null) {
            Display.error("Dormitory not found for ID: " + id);
            return;
        }

        Display.info("Leave a field blank to keep its current value.");
        String name = blankToNull(in.readLine("New name"));
        String address = blankToNull(in.readLine("New address"));
        String contact = blankToNull(in.readLine("New contact number"));
        DormType type = in.confirm("Change the dormitory type?")
                ? in.readEnum("New type", DormType.class)
                : null;

        service.updateDormitory(id, name, address, type, contact);
        Display.success("Dormitory " + id + " updated.");
    }

    private void delete() {
        List<Dormitory> dormitories = service.listDormitories();
        if (dormitories == null || dormitories.isEmpty()) {
            Display.info("No dormitories available to delete.");
            return;
        }

        Display.print(tables.dormitories(dormitories));
        String id = in.readId("Dormitory ID to delete");
        Dormitory dormitory = service.getDormitory(id);
        if (dormitory == null) {
            Display.error("Dormitory not found for ID: " + id);
            return;
        }

        if (in.confirm("Delete " + dormitory.getName() + "?")) {
            service.deleteDormitory(id);
            Display.success("Dormitory deleted.");
        } else {
            Display.info("Cancelled.");
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}