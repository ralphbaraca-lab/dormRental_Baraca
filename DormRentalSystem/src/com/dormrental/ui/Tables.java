package com.dormrental.ui;

import com.dormrental.config.AppContext;
import com.dormrental.exception.EntityNotFoundException;
import com.dormrental.model.Dormitory;
import com.dormrental.model.Lease;
import com.dormrental.model.MaintenanceRequest;
import com.dormrental.model.Payment;
import com.dormrental.model.Room;
import com.dormrental.model.Tenant;
import com.dormrental.model.User;
import com.dormrental.model.VisitorLog;
import com.dormrental.util.DateUtil;
import com.dormrental.util.MoneyUtil;
import com.dormrental.util.TextTable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Turns lists of model objects into printable tables, looking up names for ids where useful. */
public class Tables {
    private static final String YES = "yes";
    private static final String NO = "no";
    private static final String STAFF = "Staff";
    private static final String STILL_INSIDE = "(inside)";

    private final AppContext ctx;

    public Tables(AppContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
    }

    // ---------------------------------------------------------------- tables

    public String dormitories(List<Dormitory> list) {
        return render(list,
                new String[] {"ID", "Name", "Type", "Address", "Contact", "Rooms"},
                d -> new Object[] {
                        d.getId(), d.getName(), d.getType(), d.getAddress(), d.getContactNumber(),
                        ctx.getRoomService().listByDormitory(d.getId()).size()
                });
    }

    public String rooms(List<Room> list) {
        Function<String, String> dormName = memoize(this::dormitoryName);
        return render(list,
                new String[] {"ID", "Dormitory", "Room", "Floor", "Type", "Rate/bed", "Beds", "Status"},
                r -> new Object[] {
                        r.getId(), dormName.apply(r.getDormitoryId()), r.getRoomNumber(), r.getFloor(),
                        r.getType().getLabel(), MoneyUtil.format(r.getMonthlyRate()),
                        r.getOccupancy() + "/" + r.getCapacity(), r.getStatus()
                });
    }

    public String tenants(List<Tenant> list) {
        return render(list,
                new String[] {"ID", "Name", "Gender", "Email", "Phone", "School", "Active"},
                t -> new Object[] {
                        t.getId(), t.getFullName(), t.getGender(), t.getEmail(), t.getPhone(),
                        t.getSchool(), t.isActive() ? YES : NO
                });
    }

    public String leases(List<Lease> list) {
        Function<String, String> tenant = memoize(this::tenantName);
        Function<String, String> room = roomLabels();
        return render(list,
                new String[] {"ID", "Tenant", "Room", "Start", "End", "Rate/month", "Deposit", "Status"},
                l -> new Object[] {
                        l.getId(), tenant.apply(l.getTenantId()), room.apply(l.getRoomId()),
                        DateUtil.format(l.getStartDate()), DateUtil.format(l.getEndDate()),
                        MoneyUtil.format(l.getMonthlyRate()), MoneyUtil.format(l.getSecurityDeposit()),
                        l.getStatus()
                });
    }

    public String payments(List<Payment> list) {
        Function<String, String> tenant = memoize(this::tenantName);
        return render(list,
                new String[] {"ID", "Tenant", "Type", "Description", "Due", "Amount", "Status", "Paid on", "Method"},
                p -> new Object[] {
                        p.getId(), tenant.apply(p.getTenantId()), p.getType(), p.getDescription(),
                        DateUtil.format(p.getDueDate()), MoneyUtil.format(p.getAmount()), p.getStatus(),
                        DateUtil.format(p.getPaidDate()), p.getMethod()
                });
    }

    public String requests(List<MaintenanceRequest> list) {
        Function<String, String> tenant = memoize(this::tenantName);
        Function<String, String> room = roomLabels();
        return render(list,
                new String[] {"ID", "Room", "Title", "Priority", "Status", "Reported", "Assigned to", "Reported by"},
                m -> new Object[] {
                        m.getId(), room.apply(m.getRoomId()), m.getTitle(), m.getPriority(), m.getStatus(),
                        DateUtil.format(m.getReportedOn()), m.getAssignedTo(),
                        m.getTenantId() == null ? STAFF : tenant.apply(m.getTenantId())
                });
    }

    public String visitors(List<VisitorLog> list) {
        Function<String, String> tenant = memoize(this::tenantName);
        return render(list,
                new String[] {"ID", "Visitor", "Visiting", "Purpose", "Time in", "Time out"},
                v -> new Object[] {
                        v.getId(), v.getVisitorName(), tenant.apply(v.getTenantId()), v.getPurpose(),
                        DateUtil.format(v.getTimeIn()),
                        v.isInside() ? STILL_INSIDE : DateUtil.format(v.getTimeOut())
                });
    }

    public String users(List<User> list) {
        return render(list,
                new String[] {"ID", "Username", "Full name", "Role"},
                u -> new Object[] {u.getId(), u.getUsername(), u.getFullName(), u.getRole()});
    }

    // ---------------------------------------------------------- id -> label

    public String dormitoryName(String dormitoryId) {
        return nameOrId(dormitoryId, () -> ctx.getDormitoryService().getDormitory(dormitoryId).getName());
    }

    public String tenantName(String tenantId) {
        return ctx.getTenantService().getNameOrId(tenantId);
    }

    /** "Sunrise Hall 101" for a room id. */
    public String roomLabel(String roomId) {
        return roomLabel(roomId, this::dormitoryName);
    }

    // --------------------------------------------------------------- helpers

    /** A lookup that can fail with {@link EntityNotFoundException}. */
    @FunctionalInterface
    private interface Lookup {
        String get() throws EntityNotFoundException;
    }

    /** Returns the looked-up name, or the raw id if the entity no longer exists. */
    private static String nameOrId(String id, Lookup lookup) {
        try {
            return lookup.get();
        } catch (EntityNotFoundException e) {
            return id;
        }
    }

    private String roomLabel(String roomId, Function<String, String> dormName) {
        return nameOrId(roomId, () -> {
            Room room = ctx.getRoomService().getRoom(roomId);
            return dormName.apply(room.getDormitoryId()) + " " + room.getRoomNumber();
        });
    }

    /** Room-label lookup that remembers dormitory and room names for the duration of one render. */
    private Function<String, String> roomLabels() {
        Function<String, String> dormName = memoize(this::dormitoryName);
        return memoize(roomId -> roomLabel(roomId, dormName));
    }

    /**
     * Caches results for one table render, so a tenant or room that appears in many rows
     * is only looked up once. Deliberately not shared between renders, so data is never stale.
     */
    private static Function<String, String> memoize(Function<String, String> lookup) {
        Map<String, String> cache = new HashMap<>();
        return id -> cache.computeIfAbsent(id, lookup);
    }

    private static <T> String render(List<T> items, String[] headers, Function<T, Object[]> toRow) {
        TextTable table = new TextTable(headers);
        for (T item : items) {
            table.addRow(toRow.apply(item));
        }
        return table.render();
    }
}