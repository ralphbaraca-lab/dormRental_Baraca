package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.model.Lease;
import com.dormrental.model.Room;
import com.dormrental.service.LeaseService;
import com.dormrental.util.DateUtil;
import com.dormrental.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LeaseMenu extends Menu {
    private static final int EXPIRING_SOON_DAYS = 30;

    private final LeaseService service;

    public LeaseMenu(InputHelper in, AppContext ctx) {
        super(in, ctx);
        this.service = ctx.getLeaseService();
    }

    @Override
    protected String title() {
        return "LEASES";
    }

    @Override
    protected String[] options() {
        return new String[]{
                "Create lease (move a tenant in)",
                "List active leases",
                "List all leases",
                "View lease and its bills",
                "Renew lease",
                "Terminate lease early",
                "Leases ending within " + EXPIRING_SOON_DAYS + " days"
        };
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                create();
                break;
            case 2:
                Display.print(tables.leases(service.listActive()));
                break;
            case 3:
                Display.print(tables.leases(service.listAll()));
                break;
            case 4:
                view();
                break;
            case 5:
                renew();
                break;
            case 6:
                terminate();
                break;
            case 7:
                Display.print(tables.leases(service.findExpiringWithin(EXPIRING_SOON_DAYS, LocalDate.now())));
                break;
            default:
                break;
        }
    }

    private void create() {
        Display.subheader("Active tenants");
        Display.print(tables.tenants(ctx.getTenantService().listActive()));
        String tenantId = in.readId("Tenant ID");
        ctx.getTenantService().getTenant(tenantId);

        Display.subheader("Rooms with a free bed");
        Display.print(tables.rooms(ctx.getRoomService().listAvailable()));
        String roomId = in.readId("Room ID");
        Room room = ctx.getRoomService().getRoom(roomId);

        LocalDate start = in.readDateOrDefault("Start date", LocalDate.now());
        int months = in.readInt("Lease length in months", 1, LeaseService.MAX_LEASE_MONTHS);
        BigDecimal deposit = in.readOptionalMoney("Security deposit in PHP (blank = one month's rent, "
                + MoneyUtil.format(room.getMonthlyRate()) + ")");
        if (deposit == null) {
            deposit = room.getMonthlyRate();
        }

        Display.subheader("Please confirm");
        Display.info("  Tenant  : " + tables.tenantName(tenantId));
        Display.info("  Room    : " + tables.roomLabel(roomId) + " at " + MoneyUtil.format(room.getMonthlyRate()) + " per month");
        Display.info("  Period  : " + DateUtil.format(start) + " for " + months + " month(s)");
        Display.info("  Deposit : " + MoneyUtil.format(deposit));
        if (in.confirm("Create this lease?")) {
            Lease lease = service.createLease(tenantId, roomId, start, months, deposit);
            Display.success("Lease " + lease.getId() + " created. Bills were generated for the deposit and each month's rent.");
        } else {
            Display.info("Cancelled.");
        }
    }

    private void view() {
        String id = in.readId("Lease ID");
        Lease lease = service.getLease(id);
        Display.subheader("Lease " + lease.getId());
        Display.info("  Tenant : " + tables.tenantName(lease.getTenantId()) + " (" + lease.getTenantId() + ")");
        Display.info("  Room   : " + tables.roomLabel(lease.getRoomId()));
        Display.info("  Period : " + DateUtil.format(lease.getStartDate()) + " to " + DateUtil.format(lease.getEndDate()));
        Display.info("  Rate   : " + MoneyUtil.format(lease.getMonthlyRate()) + " per month, deposit "
                + MoneyUtil.format(lease.getSecurityDeposit()));
        Display.info("  Status : " + lease.getStatus());
        if (lease.getTerminatedOn() != null) {
            Display.info("  Ended  : " + DateUtil.format(lease.getTerminatedOn()) + " - " + lease.getTerminationReason());
        }
        Display.subheader("Bills");
        Display.print(tables.payments(ctx.getPaymentService().listByLease(id)));
    }

    private void renew() {
        Display.print(tables.leases(service.listActive()));
        String id = in.readId("Lease ID to renew");
        Lease lease = service.getLease(id);
        int months = in.readInt("Extra months", 1, LeaseService.MAX_LEASE_MONTHS);
        service.renewLease(id, months);
        Display.success("Lease " + id + " extended to " + DateUtil.format(lease.getEndDate()) + ".");
    }

    private void terminate() {
        Display.print(tables.leases(service.listActive()));
        String id = in.readId("Lease ID to terminate");
        Lease lease = service.getLease(id);
        String reason = in.readRequired("Reason");
        LocalDate effective = in.readDateOrDefault("Effective date", LocalDate.now());
        if (in.confirm("Terminate the lease of " + tables.tenantName(lease.getTenantId()) + "? Unpaid future rent will be cancelled.")) {
            service.terminateLease(id, reason, effective);
            Display.success("Lease " + id + " terminated and the bed is free again.");
        } else {
            Display.info("Cancelled.");
        }
    }
}
