package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.enums.Gender;
import com.dormrental.model.Lease;
import com.dormrental.model.Tenant;
import com.dormrental.service.TenantService;
import com.dormrental.util.DateUtil;
import com.dormrental.util.MoneyUtil;

import java.time.LocalDate;
import java.util.Optional;

public class TenantMenu extends Menu {
    private final TenantService service;

    public TenantMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.service = ctx.getTenantService();
    }

    @Override
    protected String title() {
        return "TENANTS";
    }

    @Override
    protected String[] options() {
        return new String[]{
                "Register tenant",
                "List all tenants",
                "Search tenants by name",
                "View tenant profile",
                "Update contact details",
                "Deactivate tenant",
                "Reactivate tenant"
        };
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                register();
                break;
            case 2:
                Display.print(tables.tenants(service.listTenants()));
                break;
            case 3:
                Display.print(tables.tenants(service.searchByName(in.readRequired("Name contains"))));
                break;
            case 4:
                profile();
                break;
            case 5:
                updateContact();
                break;
            case 6:
                deactivate();
                break;
            case 7:
                reactivate();
                break;
            default:
                break;
        }
    }

    private void register() {
        String first = in.readRequired("First name");
        String last = in.readRequired("Last name");
        String email = in.readEmail("Email");
        String phone = in.readPhone("Phone");
        Gender gender = in.readEnum("Gender", Gender.class);
        String school = in.readLine("School / workplace (optional)");
        String address = in.readLine("Home address (optional)");
        String guardianName = in.readLine("Guardian name (optional)");
        String guardianPhone = in.readOptionalPhone("Guardian phone");
        Tenant tenant = service.registerTenant(first, last, email, phone, gender, school, address, guardianName, guardianPhone);
        Display.success("Tenant registered with ID " + tenant.getId() + ".");
    }

    private void profile() {
        String id = in.readId("Tenant ID");
        Tenant tenant = service.getTenant(id);
        Display.subheader(tenant.getFullName());
        Display.info("  ID         : " + tenant.getId());
        Display.info("  Gender     : " + tenant.getGender());
        Display.info("  Email      : " + tenant.getEmail());
        Display.info("  Phone      : " + tenant.getPhone());
        Display.info("  School     : " + orDash(tenant.getSchool()));
        Display.info("  Home       : " + orDash(tenant.getHomeAddress()));
        Display.info("  Guardian   : " + orDash(tenant.getGuardianName()) + " (" + orDash(tenant.getGuardianPhone()) + ")");
        Display.info("  Registered : " + DateUtil.format(tenant.getRegisteredOn()));
        Display.info("  Status     : " + (tenant.isActive() ? "active" : "deactivated"));

        Optional<Lease> lease = ctx.getLeaseService().findActiveLeaseByTenant(id);
        if (lease.isPresent()) {
            Lease l = lease.get();
            Display.info("  Room       : " + tables.roomLabel(l.getRoomId()) + " (lease " + l.getId()
                    + ", " + DateUtil.format(l.getStartDate()) + " to " + DateUtil.format(l.getEndDate()) + ")");
        } else {
            Display.info("  Room       : none (no active lease)");
        }
        Display.info("  Balance due: " + MoneyUtil.format(ctx.getPaymentService().outstandingBalance(id, LocalDate.now())));
    }

    private void updateContact() {
        Display.print(tables.tenants(service.listTenants()));
        String id = in.readId("Tenant ID to update");
        service.getTenant(id);
        Display.info("Leave a field blank to keep its current value.");
        String email = in.readLine("New email");
        String phone = in.readLine("New phone");
        String address = in.readLine("New home address");
        service.updateContact(id, email, phone, address);
        Display.success("Tenant " + id + " updated.");
    }

    private void deactivate() {
        String id = in.readId("Tenant ID to deactivate");
        Tenant tenant = service.getTenant(id);
        if (in.confirm("Deactivate " + tenant.getFullName() + "?")) {
            service.deactivate(id);
            Display.success(tenant.getFullName() + " deactivated.");
        } else {
            Display.info("Cancelled.");
        }
    }

    private void reactivate() {
        String id = in.readId("Tenant ID to reactivate");
        Tenant tenant = service.reactivate(id);
        Display.success(tenant.getFullName() + " is active again.");
    }

    private static String orDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }
}
