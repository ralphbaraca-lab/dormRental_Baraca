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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

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
        BigDecimal overdueTotal = BigDecimal.ZERO;
        for (Payment p : overdue) {
            overdueTotal = overdueTotal.add(p.getAmount());
        }
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
        List<Payment> all = paymentService.listAll();
        TextTable table = new TextTable("Type", "Billed", "Collected");
        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        for (PaymentType type : PaymentType.values()) {
            BigDecimal billed = BigDecimal.ZERO;
            BigDecimal collected = BigDecimal.ZERO;
            for (Payment p : all) {
                if (p.getType() != type) {
                    continue;
                }
                if (!p.getDueDate().isBefore(from) && !p.getDueDate().isAfter(to)) {
                    billed = billed.add(p.getAmount());
                }
                if (p.getStatus() == PaymentStatus.PAID && p.getPaidDate() != null
                        && !p.getPaidDate().isBefore(from) && !p.getPaidDate().isAfter(to)) {
                    collected = collected.add(p.getAmount());
                }
            }
            table.addRow(type, MoneyUtil.format(billed), MoneyUtil.format(collected));
            totalBilled = totalBilled.add(billed);
            totalCollected = totalCollected.add(collected);
        }
        table.addRow("TOTAL", MoneyUtil.format(totalBilled), MoneyUtil.format(totalCollected));
        return "REVENUE REPORT FOR " + month + NL + table.render();
    }

    public String overdueReport(LocalDate today) {
        TextTable table = new TextTable("Payment", "Tenant", "Type", "Description", "Due", "Days late", "Amount");
        BigDecimal total = BigDecimal.ZERO;
        for (Payment p : paymentService.listOverdue()) {
            table.addRow(p.getId(), tenantName(p.getTenantId()), p.getType(), p.getDescription(),
                    DateUtil.format(p.getDueDate()), p.daysOverdue(today), MoneyUtil.format(p.getAmount()));
            total = total.add(p.getAmount());
        }
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
        sb.append(String.format("  Receipt for : %s%n", p.getId()));
        sb.append(String.format("  Tenant      : %s%n", tenantName(p.getTenantId())));
        sb.append(String.format("  Lease       : %s%n", p.getLeaseId()));
        sb.append(String.format("  Item        : %s (%s)%n", p.getDescription(), p.getType()));
        sb.append(String.format("  Amount paid : %s%n", MoneyUtil.format(p.getAmount())));
        sb.append(String.format("  Paid on     : %s%n", DateUtil.format(p.getPaidDate())));
        sb.append(String.format("  Method      : %s%n", p.getMethod()));
        if (p.getReferenceNo() != null && !p.getReferenceNo().isEmpty()) {
            sb.append(String.format("  Reference   : %s%n", p.getReferenceNo()));
        }
        sb.append(bar).append(NL);
        return sb.toString();
    }

    private String tenantName(String tenantId) {
        try {
            return tenantService.getTenant(tenantId).getFullName();
        } catch (RuntimeException e) {
            return tenantId;
        }
    }

    private static String percent(int part, int whole) {
        return whole == 0 ? "0.0%" : String.format("%.1f%%", part * 100.0 / whole);
    }
}
