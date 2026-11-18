package com.dormrental.config;

import com.dormrental.enums.Amenity;
import com.dormrental.enums.DormType;
import com.dormrental.enums.Gender;
import com.dormrental.enums.MaintenancePriority;
import com.dormrental.enums.PaymentMethod;
import com.dormrental.enums.PaymentType;
import com.dormrental.enums.RoomType;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.model.Dormitory;
import com.dormrental.model.Lease;
import com.dormrental.model.Payment;
import com.dormrental.model.Room;
import com.dormrental.model.Tenant;
import com.dormrental.service.DormitoryService;
import com.dormrental.service.LeaseService;
import com.dormrental.service.PaymentService;
import com.dormrental.service.RoomService;
import com.dormrental.service.TenantService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Fills an empty system with made-up demo data so every menu has something to show. */
public final class DataSeeder {
    private DataSeeder() {
    }

    public static void seed(AppContext ctx) {
        if (!ctx.getDormitoryService().listDormitories().isEmpty() || !ctx.getTenantService().listTenants().isEmpty()) {
            throw new BusinessRuleException("Sample data can only be loaded into an empty system.");
        }
        try {
            load(ctx);
        } catch (RuntimeException e) {
            // Everything loaded so far is already saved, and the system is no longer empty, so loading can't simply be
            // retried. Say so, instead of leaving the user to find out from the "empty system" message next time.
            BusinessRuleException stopped = new BusinessRuleException("Loading sample data stopped part-way ("
                    + (e.getMessage() == null ? e.toString() : e.getMessage())
                    + "). Part of the demo data is now saved: delete the data folder and start again for a clean set.");
            stopped.addSuppressed(e);
            throw stopped;
        }
    }

    private static void load(AppContext ctx) {
        DormitoryService dormService = ctx.getDormitoryService();
        RoomService roomService = ctx.getRoomService();
        TenantService tenantService = ctx.getTenantService();
        LeaseService leaseService = ctx.getLeaseService();
        PaymentService paymentService = ctx.getPaymentService();

        Dormitory sunrise = dormService.addDormitory("Sunrise Hall", "12 Sample St., Cebu City", DormType.FEMALE_ONLY, "032-555-0101");
        Dormitory pioneer = dormService.addDormitory("Pioneer House", "45 Example Ave., Cebu City", DormType.MALE_ONLY, "032-555-0102");
        Dormitory unity = dormService.addDormitory("Unity Residences", "8 Demo Rd., Cebu City", DormType.CO_ED, "032-555-0103");

        Set<Amenity> basic = EnumSet.of(Amenity.WIFI, Amenity.ELECTRIC_FAN, Amenity.CLOSET);
        Set<Amenity> comfort = EnumSet.of(Amenity.WIFI, Amenity.AIR_CONDITIONING, Amenity.PRIVATE_BATHROOM, Amenity.STUDY_DESK);

        Room sunrise101 = roomService.addRoom(sunrise.getId(), "101", 1, RoomType.DOUBLE, new BigDecimal("2500"), basic);
        roomService.addRoom(sunrise.getId(), "102", 1, RoomType.QUAD, new BigDecimal("1800"), basic);
        roomService.addRoom(sunrise.getId(), "201", 2, RoomType.SINGLE, new BigDecimal("4500"), comfort);

        Room pioneer101 = roomService.addRoom(pioneer.getId(), "101", 1, RoomType.TRIPLE, new BigDecimal("2200"), basic);
        roomService.addRoom(pioneer.getId(), "102", 1, RoomType.DOUBLE, new BigDecimal("2600"), basic);
        roomService.addRoom(pioneer.getId(), "201", 2, RoomType.SINGLE, new BigDecimal("4200"), comfort);

        Room unity201 = roomService.addRoom(unity.getId(), "201", 2, RoomType.DOUBLE, new BigDecimal("3000"), comfort);
        roomService.addRoom(unity.getId(), "202", 2, RoomType.QUAD, new BigDecimal("2000"), basic);

        Tenant maria = tenantService.registerTenant("Maria", "Santos", "maria.santos@example.com", "0917 000 0001",
                Gender.FEMALE, "Sample State University", "Sample Town", "Rosa Santos", "0917 000 1001");
        Tenant ana = tenantService.registerTenant("Ana", "Reyes", "ana.reyes@example.com", "0917 000 0002",
                Gender.FEMALE, "Sample State University", "Example City", "Luis Reyes", "0917 000 1002");
        Tenant juan = tenantService.registerTenant("Juan", "Dela Cruz", "juan.delacruz@example.com", "0917 000 0003",
                Gender.MALE, "Example Institute of Technology", "Demo Province", "Elena Dela Cruz", "0917 000 1003");
        tenantService.registerTenant("Pedro", "Garcia", "pedro.garcia@example.com", "0917 000 0004",
                Gender.MALE, "Example Institute of Technology", "Demo Province", "", "");
        Tenant carla = tenantService.registerTenant("Carla", "Lim", "carla.lim@example.com", "0917 000 0005",
                Gender.FEMALE, "Sample College", "Sample Town", "Ben Lim", "0917 000 1005");

        LocalDate today = LocalDate.now();

        // Each lease takes one month's rent as the deposit, taken from the room's own rate so the two can't drift apart.

        // Maria: started two months ago and has paid the deposit and the first month.
        Lease mariaLease = leaseService.createLease(maria.getId(), sunrise101.getId(), today.minusMonths(2), 6,
                sunrise101.getMonthlyRate());
        List<Payment> mariaBills = paymentService.listByLease(mariaLease.getId());
        paymentService.recordPayment(firstBillOfType(mariaBills, PaymentType.DEPOSIT).getId(), PaymentMethod.CASH, "");
        paymentService.recordPayment(firstBillOfType(mariaBills, PaymentType.RENT).getId(), PaymentMethod.GCASH, "GC-100245");

        // Ana: started a month ago and has paid nothing yet, so her bills show up as overdue.
        leaseService.createLease(ana.getId(), sunrise101.getId(), today.minusMonths(1), 6, sunrise101.getMonthlyRate());

        leaseService.createLease(juan.getId(), pioneer101.getId(), today, 4, pioneer101.getMonthlyRate());
        leaseService.createLease(carla.getId(), unity201.getId(), today.minusDays(10), 10, unity201.getMonthlyRate());

        ctx.getMaintenanceService().report(sunrise101.getId(), maria.getId(), "Leaking faucet",
                "The bathroom faucet drips all night.", MaintenancePriority.MEDIUM);
        ctx.getMaintenanceService().report(pioneer101.getId(), juan.getId(), "Broken window latch",
                "Window will not lock.", MaintenancePriority.HIGH);
        ctx.getVisitorService().checkIn(maria.getId(), "Rosa Santos", "Family visit");

        ctx.runDailyTasks();
    }

    /** The earliest bill of that type. Picked by type, so it doesn't depend on where the list happens to put it. */
    private static Payment firstBillOfType(List<Payment> bills, PaymentType type) {
        return bills.stream()
                .filter(bill -> bill.getType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected a " + type + " bill on the demo lease."));
    }
}