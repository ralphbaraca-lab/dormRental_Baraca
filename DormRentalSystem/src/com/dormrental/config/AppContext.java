package com.dormrental.config;

import com.dormrental.exception.DormRentalException;
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
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/** Creates the repositories and services once and hands them out. This is the app's "wiring" class. */
public class AppContext {
    private static final String LOCK_FILE_NAME = ".lock";

    /**
     * Held for as long as the program runs, so no second copy can use the same data folder.
     * Never read; the field only keeps the lock (and its file) from being released early. The operating
     * system releases it when the program ends. Null if this file system doesn't support locking.
     */
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final FileLock dataDirLock;

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
        Objects.requireNonNull(dataDir, "dataDir must not be null");
        this.dataDirLock = lockDataDir(dataDir);

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

    /**
     * Each repository loads its whole file into memory and rewrites the whole file on every change.
     * Two copies of the program on the same folder would overwrite each other's changes, so the second
     * one is refused. Any other problem (a read-only folder, a file system that can't lock) is not
     * reported here: the repositories report real trouble with the data files themselves.
     */
    private static FileLock lockDataDir(String dataDir) {
        try {
            Path dir = Paths.get(dataDir);
            Files.createDirectories(dir);
            FileChannel channel = FileChannel.open(dir.resolve(LOCK_FILE_NAME),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException e) {
                lock = null; // already locked by this same program
            }
            if (lock == null) {
                try {
                    channel.close();
                } catch (IOException ignored) {
                    // nothing useful to do; the message below is the one that matters
                }
                throw new DormRentalException("The data folder '" + dataDir
                        + "' is already in use by another copy of this program. Close that copy first.", null);
            }
            return lock;
        } catch (IOException e) {
            return null;
        }
    }

    /** Closes expired leases, flags overdue bills and adds late fees. Safe to run as often as you like. */
    public String runDailyTasks() {
        LocalDate today = LocalDate.now();
        int closed = leaseService.closeExpiredLeases(today);
        int overdue = paymentService.refreshOverdue(today);
        int fees = paymentService.applyLateFees(today);
        return String.format(Locale.ROOT,
                "Daily tasks done: %d lease(s) closed, %d bill(s) marked overdue, %d late fee(s) added.",
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