package com.dormrental.config;

import com.dormrental.model.Dormitory;
import com.dormrental.model.Lease;
import com.dormrental.model.MaintenanceRequest;
import com.dormrental.model.Payment;
import com.dormrental.model.Room;
import com.dormrental.model.Tenant;
import com.dormrental.model.User;
import com.dormrental.model.VisitorLog;
import com.dormrental.repository.FileRepository;
import com.dormrental.repository.Repository;
import com.dormrental.service.AuthService;
import com.dormrental.service.DormitoryService;
import com.dormrental.service.LeaseService;
import com.dormrental.service.MaintenanceService;
import com.dormrental.service.PaymentService;
import com.dormrental.service.ReportService;
import com.dormrental.service.RoomService;
import com.dormrental.service.TenantService;
import com.dormrental.service.VisitorService;

import java.time.LocalDate;

/** Creates the repositories and services once and hands them out. This is the app's "wiring" class. */
public class AppContext {
    private final AuthService authService;
    private final DormitoryService dormitoryService;
    private final RoomService roomService;
    private final TenantService tenantService;
    private final PaymentService paymentService;
    private final LeaseService leaseService;
    private final MaintenanceService maintenanceService;
    private final VisitorService visitorService;
    private final ReportService reportService;

    public AppContext(String dataDir) {
        Repository<User> users = new FileRepository<>(dataDir, "users", "USR");
        Repository<Dormitory> dormitories = new FileRepository<>(dataDir, "dormitories", "DORM");
        Repository<Room> rooms = new FileRepository<>(dataDir, "rooms", "RM");
        Repository<Tenant> tenants = new FileRepository<>(dataDir, "tenants", "TNT");
        Repository<Lease> leases = new FileRepository<>(dataDir, "leases", "LSE");
        Repository<Payment> payments = new FileRepository<>(dataDir, "payments", "PAY");
        Repository<MaintenanceRequest> requests = new FileRepository<>(dataDir, "maintenance", "MNT");
        Repository<VisitorLog> visitors = new FileRepository<>(dataDir, "visitors", "VIS");

        authService = new AuthService(users);
        dormitoryService = new DormitoryService(dormitories, rooms);
        roomService = new RoomService(rooms, dormitoryService);
        tenantService = new TenantService(tenants, leases);
        paymentService = new PaymentService(payments);
        leaseService = new LeaseService(leases, tenantService, roomService, dormitoryService, paymentService);
        maintenanceService = new MaintenanceService(requests, roomService, tenantService);
        visitorService = new VisitorService(visitors, tenantService);
        reportService = new ReportService(dormitoryService, roomService, tenantService,
                leaseService, paymentService, maintenanceService);
    }

    /** Closes expired leases, flags overdue bills and adds late fees. Safe to run as often as you like. */
    public String runDailyTasks() {
        LocalDate today = LocalDate.now();
        int closed = leaseService.closeExpiredLeases(today);
        int overdue = paymentService.refreshOverdue(today);
        int fees = paymentService.applyLateFees(today);
        return String.format("Daily tasks done: %d lease(s) closed, %d bill(s) marked overdue, %d late fee(s) added.",
                closed, overdue, fees);
    }

    public AuthService getAuthService() {
        return authService;
    }

    public DormitoryService getDormitoryService() {
        return dormitoryService;
    }

    public RoomService getRoomService() {
        return roomService;
    }

    public TenantService getTenantService() {
        return tenantService;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public LeaseService getLeaseService() {
        return leaseService;
    }

    public MaintenanceService getMaintenanceService() {
        return maintenanceService;
    }

    public VisitorService getVisitorService() {
        return visitorService;
    }

    public ReportService getReportService() {
        return reportService;
    }
}
