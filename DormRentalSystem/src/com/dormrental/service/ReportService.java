package com.dormrental.service;

import com.dormrental.enums.PaymentStatus;
import com.dormrental.enums.PaymentType;
import com.dormrental.exception.BusinessRuleException;
import com.dormrental.model.Dormitory;
import com.dormrental.model.Payment;
import com.dormrental.model.Room;
import com.dormrental.model.Tenant;
import com.dormrental.util.DateUtil;
import com.dormrental.util.MoneyUtil;
import com.dormrental.util.TextTable;
import com.dormrental.util.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** Builds printable text reports from the other services. Nothing here changes data. */
public class ReportService {
    private static final String NL = System.lineSeparator();

    private final DormitoryService dormitoryService;
    private final RoomService roomService;
    private final TenantService tenantService;
    private final LeaseService leaseService;
    private final PaymentService paymentService;
    private final MaintenanceService maintenanceService;

    public ReportService(DormitoryService dormitoryService, RoomService roomService, TenantService tenantService,
                         LeaseService leaseService, PaymentService paymentService, MaintenanceService maintenanceService) {
        this.dormitoryService = dormitoryService;
        this.roomService = roomService;
        this.tenantService = tenantService;
        this.leaseService = leaseService;
        this.paymentService = paymentService;
        this.maintenanceService = maintenanceService;
    }

    /** A few headline numbers for the main menu. */
    public String dashboard() {
        int beds = roomService.totalBeds();
        int occupied = roomService.occupiedBeds();
        List<Payment> overdue = paymentService.listOverdue();
        BigDecimal overdueTotal = sum(overdue.stream().map(Payment::getAmount));
        return String.format("  Dormitories: %d | Rooms: %d | Beds occupied: %d/%d (%s)%n"
                        + "  Active tenants: %d | Active leases: %d%n"
                        + "  Overdue bills: %d (%s) | Open maintenance requests: %d",
                dormitoryService.listDormitories().size(), roomService.listRooms().size(), occupied, beds, percent(occupied, beds),
                tenantService.listActive().size(), leaseService.listActive().size(),
                overdue.size(), MoneyUtil.format(overdueTotal), maintenanceService.listOpen().size());
    }

    public String occupancyReport() {
        TextTable table = new TextTable("Dormitory", "Type", "Rooms", "Beds", "Occupied", "Vacant", "Occupancy");
        int totalRooms = 0;
        int totalBeds = 0;
        int totalOccupied = 0;
        for (Dormitory dormitory : dormitoryService.listDormitories()) {
            List<Room> rooms = roomService.listByDormitory(dormitory.getId());
            int beds = 0;
            int occupied = 0;
            for (Room room : rooms) {
                beds += room.getCapacity();
                occupied += room.getOccupancy();
            }
            table.addRow(dormitory.getName(), dormitory.getType(), rooms.size(), beds, occupied, beds - occupied,
                    percent(occupied, beds));
            totalRooms += rooms.size();
            totalBeds += beds;
            totalOccupied += occupied;
        }
        table.addRow("TOTAL", "", totalRooms, totalBeds, totalOccupied, totalBeds - totalOccupied,
                percent(totalOccupied, totalBeds));
        return "OCCUPANCY REPORT" + NL + table.render();
    }

    /** Billed = bills that fall due in the month. Collected = money actually received in the month. */
    public String revenueReport(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        // One pass over the payments, adding each one to its type's totals.
        Map<PaymentType, BigDecimal> billed = zeroPerType();
        Map<PaymentType, BigDecimal> collected = zeroPerType();
        for (Payment p : paymentService.listAll()) {
            if (isWithin(p.getDueDate(), from, to)) {
                billed.merge(p.getType(), p.getAmount(), BigDecimal::add);
            }
            if (p.getStatus() == PaymentStatus.PAID && p.getPaidDate() != null && isWithin(p.getPaidDate(), from, to)) {
                collected.merge(p.getType(), p.getAmount(), BigDecimal::add);
            }
        }

        TextTable table = new TextTable("Type", "Billed", "Collected");
        for (PaymentType type : PaymentType.values()) {
            table.addRow(type, MoneyUtil.format(billed.get(type)), MoneyUtil.format(collected.get(type)));
        }
        table.addRow("TOTAL", MoneyUtil.format(sum(billed.values().stream())),
                MoneyUtil.format(sum(collected.values().stream())));
        return "REVENUE REPORT FOR " + month + NL + table.render();
    }

    public String overdueReport(LocalDate today) {
        TextTable table = new TextTable("Payment", "Tenant", "Type", "Description", "Due", "Days late", "Amount");
        Map<String, String> tenantNames = new HashMap<>(); // each tenant is looked up once, however many bills they have
        List<Payment> overdue = paymentService.listOverdue();
        for (Payment p : overdue) {
            table.addRow(p.getId(), tenantNames.computeIfAbsent(p.getTenantId(), this::tenantName), p.getType(),
                    p.getDescription(), DateUtil.format(p.getDueDate()), p.daysOverdue(today),
                    MoneyUtil.format(p.getAmount()));
        }
        BigDecimal total = sum(overdue.stream().map(Payment::getAmount));
        return "OVERDUE PAYMENTS" + NL + table.render() + NL + "  Total overdue: " + MoneyUtil.format(total) + NL;
    }

    public String tenantLedger(String tenantId, LocalDate today) {
        Tenant tenant = tenantService.getTenant(tenantId);
        TextTable table = new TextTable("Payment", "Type", "Description", "Due", "Amount", "Status", "Paid on");
        for (Payment p : paymentService.listByTenant(tenantId)) {
            table.addRow(p.getId(), p.getType(), p.getDescription(), DateUtil.format(p.getDueDate()),
                    MoneyUtil.format(p.getAmount()), p.getStatus(), DateUtil.format(p.getPaidDate()));
        }
        return "LEDGER: " + tenant.getFullName() + " (" + tenant.getId() + ")" + NL + table.render() + NL
                + "  Outstanding balance (due to date): "
                + MoneyUtil.format(paymentService.outstandingBalance(tenantId, today)) + NL;
    }

    public String receipt(String paymentId) {
        Payment p = paymentService.getPayment(paymentId);
        if (p.getStatus() != PaymentStatus.PAID) {
            throw new BusinessRuleException("Payment " + paymentId + " has not been paid yet, so there is no receipt.");
        }
        String bar = "  ----------------------------------------";
        StringBuilder sb = new StringBuilder();
        sb.append("  OFFICIAL RECEIPT").append(NL).append(bar).append(NL);
        appendLine(sb, "Receipt for", p.getId());
        appendLine(sb, "Tenant", tenantName(p.getTenantId()));
        appendLine(sb, "Lease", p.getLeaseId());
        appendLine(sb, "Item", p.getDescription() + " (" + p.getType() + ")");
        appendLine(sb, "Amount paid", MoneyUtil.format(p.getAmount()));
        appendLine(sb, "Paid on", DateUtil.format(p.getPaidDate()));
        appendLine(sb, "Method", p.getMethod());
        if (!Validator.isBlank(p.getReferenceNo())) {
            appendLine(sb, "Reference", p.getReferenceNo());
        }
        sb.append(bar).append(NL);
        return sb.toString();
    }

    // ---- helpers -------------------------------------------------------------------------

    private static void appendLine(StringBuilder sb, String label, Object value) {
        sb.append(String.format("  %-12s: %s%n", label, value));
    }

    private String tenantName(String tenantId) {
        return tenantService.getNameOrId(tenantId);
    }

    private static Map<PaymentType, BigDecimal> zeroPerType() {
        Map<PaymentType, BigDecimal> map = new EnumMap<>(PaymentType.class);
        for (PaymentType type : PaymentType.values()) {
            map.put(type, BigDecimal.ZERO);
        }
        return map;
    }

    private static boolean isWithin(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private static BigDecimal sum(Stream<BigDecimal> amounts) {
        return amounts.reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String percent(int part, int whole) {
        // Locale.US so the decimal point doesn't turn into a comma on a computer set to another region.
        return whole == 0 ? "0.0%" : String.format(Locale.US, "%.1f%%", part * 100.0 / whole);
    }
}