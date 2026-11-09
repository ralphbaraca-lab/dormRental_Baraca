package com.dormrental;

import com.dormrental.config.AppContext;
import com.dormrental.config.DataSeeder;
import com.dormrental.enums.Gender;
import com.dormrental.enums.MaintenanceStatus;
import com.dormrental.enums.PaymentMethod;
import com.dormrental.enums.PaymentStatus;
import com.dormrental.enums.PaymentType;
import com.dormrental.exception.AuthenticationException;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.exception.DuplicateEntityException;
import com.dormrental.exception.ValidationException;
import com.dormrental.model.Lease;
import com.dormrental.model.MaintenanceRequest;
import com.dormrental.model.Payment;
import com.dormrental.model.Room;
import com.dormrental.model.Tenant;
import com.dormrental.model.VisitorLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Plain-Java smoke test (no JUnit needed). Run it after compiling; it exits with code 1 if any check fails.
 * It works on a throw-away data folder, so your real data is never touched.
 */
public class ServiceSmokeTest {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("dormrental-test");
        try {
            runAll(dir.toString());
        } finally {
            try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        System.out.printf("%n%d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runAll(String dir) {
        AppContext ctx = new AppContext(dir);

        // ---- login ----
        expect("wrong password is rejected", AuthenticationException.class,
                () -> ctx.getAuthService().login("admin", "nope"));
        ctx.getAuthService().login("admin", "admin123");
        check("admin can log in", ctx.getAuthService().isAdmin());

        // ---- seed data ----
        DataSeeder.seed(ctx);
        check("seed: 3 dormitories", ctx.getDormitoryService().listDormitories().size() == 3);
        check("seed: 8 rooms", ctx.getRoomService().listRooms().size() == 8);
        check("seed: 5 tenants", ctx.getTenantService().listTenants().size() == 5);
        check("seed: 4 active leases", ctx.getLeaseService().listActive().size() == 4);
        expect("seeding twice is blocked", BusinessRuleException.class, () -> DataSeeder.seed(ctx));

        Tenant maria = findTenant(ctx, "Maria");
        Tenant ana = findTenant(ctx, "Ana");
        Tenant juan = findTenant(ctx, "Juan");
        Tenant pedro = findTenant(ctx, "Pedro");
        Tenant carla = findTenant(ctx, "Carla");
        Room sunrise101 = findRoom(ctx, "Sunrise Hall", "101");
        Room sunrise102 = findRoom(ctx, "Sunrise Hall", "102");
        Room pioneer102 = findRoom(ctx, "Pioneer House", "102");

        // ---- lease rules ----
        expect("male tenant cannot enter a female-only dorm", BusinessRuleException.class,
                () -> ctx.getLeaseService().createLease(pedro.getId(), sunrise102.getId(), LocalDate.now(), 3, new java.math.BigDecimal("1000")));
        expect("tenant with an active lease cannot lease again", BusinessRuleException.class,
                () -> ctx.getLeaseService().createLease(maria.getId(), sunrise102.getId(), LocalDate.now(), 3, new java.math.BigDecimal("1000")));

        Tenant newcomer = ctx.getTenantService().registerTenant("Lia", "Tan", "lia.tan@example.com", "0917 000 0009",
                Gender.FEMALE, "", "", "", "");
        try {
            ctx.getLeaseService().createLease(newcomer.getId(), sunrise101.getId(), LocalDate.now(), 3, new java.math.BigDecimal("1000"));
            check("full room is rejected", false);
        } catch (BusinessRuleException e) {
            check("full room is rejected", e.getMessage().contains("full"));
        }

        ctx.getRoomService().setUnderMaintenance(sunrise102.getId(), true);
        expect("room under maintenance is rejected", BusinessRuleException.class,
                () -> ctx.getLeaseService().createLease(newcomer.getId(), sunrise102.getId(), LocalDate.now(), 3, new java.math.BigDecimal("1000")));
        ctx.getRoomService().setUnderMaintenance(sunrise102.getId(), false);

        Lease newLease = ctx.getLeaseService().createLease(newcomer.getId(), sunrise102.getId(), LocalDate.now(), 3, new java.math.BigDecimal("1000"));
        check("new lease moves the tenant in", ctx.getRoomService().getRoom(sunrise102.getId()).getOccupancy() == 1);
        check("new lease creates deposit + 3 rent bills", ctx.getPaymentService().listByLease(newLease.getId()).size() == 4);
        expect("tenant with active lease cannot be deactivated", BusinessRuleException.class,
                () -> ctx.getTenantService().deactivate(newcomer.getId()));

        // ---- billing ----
        long fees = ctx.getPaymentService().listAll().stream().filter(p -> p.getType() == PaymentType.PENALTY).count();
        check("late fees were created for overdue rent", fees > 0);
        check("overdue list is not empty", !ctx.getPaymentService().listOverdue().isEmpty());
        check("daily tasks are idempotent", ctx.runDailyTasks().contains("0 late fee(s)"));
        check("overdue tenant owes money", ctx.getPaymentService().outstandingBalance(ana.getId(), LocalDate.now()).signum() > 0);

        Payment toPay = ctx.getPaymentService().listUnpaidByTenant(ana.getId()).get(0);
        ctx.getPaymentService().recordPayment(toPay.getId(), PaymentMethod.CASH, "TEST-1");
        check("payment is marked PAID", ctx.getPaymentService().getPayment(toPay.getId()).getStatus() == PaymentStatus.PAID);
        expect("paying twice is rejected", BusinessRuleException.class,
                () -> ctx.getPaymentService().recordPayment(toPay.getId(), PaymentMethod.CASH, ""));
        check("receipt mentions the tenant", ctx.getReportService().receipt(toPay.getId()).contains("Ana Reyes"));

        // ---- renew / terminate ----
        Lease carlaLease = ctx.getLeaseService().findActiveLeaseByTenant(carla.getId()).get();
        int billsBefore = ctx.getPaymentService().listByLease(carlaLease.getId()).size();
        LocalDate endBefore = carlaLease.getEndDate();
        ctx.getLeaseService().renewLease(carlaLease.getId(), 2);
        check("renewal adds 2 rent bills", ctx.getPaymentService().listByLease(carlaLease.getId()).size() == billsBefore + 2);
        check("renewal pushes the end date out", ctx.getLeaseService().getLease(carlaLease.getId()).getEndDate().isAfter(endBefore));

        Lease juanLease = ctx.getLeaseService().findActiveLeaseByTenant(juan.getId()).get();
        ctx.getLeaseService().terminateLease(juanLease.getId(), "Moving out", LocalDate.now());
        long juanRent = ctx.getPaymentService().listByLease(juanLease.getId()).stream()
                .filter(p -> p.getType() == PaymentType.RENT).count();
        check("termination cancels future unpaid rent", juanRent == 1);
        check("termination frees the bed", ctx.getRoomService().getRoom(juanLease.getRoomId()).getOccupancy() == 0);
        ctx.getTenantService().deactivate(juan.getId());
        check("tenant can be deactivated after the lease ends", !ctx.getTenantService().getTenant(juan.getId()).isActive());
        expect("deactivated tenant cannot lease", BusinessRuleException.class,
                () -> ctx.getLeaseService().createLease(juan.getId(), pioneer102.getId(), LocalDate.now(), 3, new java.math.BigDecimal("1000")));

        // ---- maintenance & visitors ----
        List<MaintenanceRequest> open = ctx.getMaintenanceService().listOpen();
        check("most urgent request comes first", open.get(0).getTitle().equals("Broken window latch"));
        MaintenanceRequest resolved = ctx.getMaintenanceService()
                .updateStatus(open.get(0).getId(), MaintenanceStatus.RESOLVED, "Replaced latch");
        check("resolving sets the resolved date", resolved.getResolvedOn() != null);
        check("resolved request leaves the open list", ctx.getMaintenanceService().listOpen().size() == open.size() - 1);

        VisitorLog visit = ctx.getVisitorService().listInside().get(0);
        ctx.getVisitorService().checkOut(visit.getId());
        check("visitor check-out works", ctx.getVisitorService().listInside().isEmpty());

        // ---- validation ----
        expect("bad email is rejected", ValidationException.class,
                () -> ctx.getTenantService().registerTenant("A", "B", "not-an-email", "0917 000 0010", Gender.MALE, "", "", "", ""));
        expect("duplicate email is rejected", DuplicateEntityException.class,
                () -> ctx.getTenantService().registerTenant("A", "B", "maria.santos@example.com", "0917 000 0010", Gender.MALE, "", "", "", ""));
        expect("dormitory with rooms cannot be deleted", BusinessRuleException.class,
                () -> ctx.getDormitoryService().deleteDormitory(sunrise101.getDormitoryId()));

        // ---- reports ----
        check("occupancy report has a TOTAL row", ctx.getReportService().occupancyReport().contains("TOTAL"));
        check("revenue report renders", ctx.getReportService().revenueReport(YearMonth.now()).contains("REVENUE REPORT"));
        check("overdue report renders", ctx.getReportService().overdueReport(LocalDate.now()).contains("Total overdue"));
        check("ledger renders", ctx.getReportService().tenantLedger(maria.getId(), LocalDate.now()).contains("Maria Santos"));

        // ---- persistence: a brand-new context must see the same data ----
        AppContext reloaded = new AppContext(dir);
        check("data survives a restart (tenants)", reloaded.getTenantService().listTenants().size() == ctx.getTenantService().listTenants().size());
        check("data survives a restart (payments)", reloaded.getPaymentService().listAll().size() == ctx.getPaymentService().listAll().size());
        check("data survives a restart (room occupancy)",
                reloaded.getRoomService().getRoom(sunrise102.getId()).getOccupancy() == 1);
        reloaded.getAuthService().login("admin", "admin123");
        check("password hash is not stored as plain text",
                !reloaded.getAuthService().getCurrentUser().getPasswordHash().contains("admin123"));
    }

    // ---- tiny test helpers ----

    private static Tenant findTenant(AppContext ctx, String firstName) {
        return ctx.getTenantService().searchByName(firstName).get(0);
    }

    private static Room findRoom(AppContext ctx, String dormName, String roomNumber) {
        for (Room room : ctx.getRoomService().listRooms()) {
            if (room.getRoomNumber().equals(roomNumber)
                    && ctx.getDormitoryService().getDormitory(room.getDormitoryId()).getName().equals(dormName)) {
                return room;
            }
        }
        throw new IllegalStateException("Test setup problem: no room " + roomNumber + " in " + dormName);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    private static void expect(String name, Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  FAIL  " + name + " (no exception thrown)");
        } catch (Throwable t) {
            if (expected.isInstance(t)) {
                passed++;
                System.out.println("  PASS  " + name);
            } else {
                failed++;
                System.out.println("  FAIL  " + name + " (got " + t + ")");
            }
        }
    }
}
