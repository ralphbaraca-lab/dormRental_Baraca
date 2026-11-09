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

import java.util.List;

/** Turns lists of model objects into printable tables, looking up names for ids where useful. */
public class Tables {
    private final AppContext ctx;

    public Tables(AppContext ctx) {
        this.ctx = ctx;
    }

    public String dormitories(List<Dormitory> list) {
        TextTable table = new TextTable("ID", "Name", "Type", "Address", "Contact", "Rooms");
        for (Dormitory d : list) {
            table.addRow(d.getId(), d.getName(), d.getType(), d.getAddress(), d.getContactNumber(),
                    ctx.getRoomService().listByDormitory(d.getId()).size());
        }
        return table.render();
    }

    public String rooms(List<Room> list) {
        TextTable table = new TextTable("ID", "Dormitory", "Room", "Floor", "Type", "Rate/bed", "Beds", "Status");
        for (Room r : list) {
            table.addRow(r.getId(), dormitoryName(r.getDormitoryId()), r.getRoomNumber(), r.getFloor(),
                    r.getType().getLabel(), MoneyUtil.format(r.getMonthlyRate()),
                    r.getOccupancy() + "/" + r.getCapacity(), r.getStatus());
        }
        return table.render();
    }

    public String tenants(List<Tenant> list) {
        TextTable table = new TextTable("ID", "Name", "Gender", "Email", "Phone", "School", "Active");
        for (Tenant t : list) {
            table.addRow(t.getId(), t.getFullName(), t.getGender(), t.getEmail(), t.getPhone(),
                    t.getSchool(), t.isActive() ? "yes" : "no");
        }
        return table.render();
    }

    public String leases(List<Lease> list) {
        TextTable table = new TextTable("ID", "Tenant", "Room", "Start", "End", "Rate/month", "Deposit", "Status");
        for (Lease l : list) {
            table.addRow(l.getId(), tenantName(l.getTenantId()), roomLabel(l.getRoomId()),
                    DateUtil.format(l.getStartDate()), DateUtil.format(l.getEndDate()),
                    MoneyUtil.format(l.getMonthlyRate()), MoneyUtil.format(l.getSecurityDeposit()), l.getStatus());
        }
        return table.render();
    }

    public String payments(List<Payment> list) {
        TextTable table = new TextTable("ID", "Tenant", "Type", "Description", "Due", "Amount", "Status", "Paid on", "Method");
        for (Payment p : list) {
            table.addRow(p.getId(), tenantName(p.getTenantId()), p.getType(), p.getDescription(),
                    DateUtil.format(p.getDueDate()), MoneyUtil.format(p.getAmount()), p.getStatus(),
                    DateUtil.format(p.getPaidDate()), p.getMethod());
        }
        return table.render();
    }

    public String requests(List<MaintenanceRequest> list) {
        TextTable table = new TextTable("ID", "Room", "Title", "Priority", "Status", "Reported", "Assigned to", "Reported by");
        for (MaintenanceRequest m : list) {
            table.addRow(m.getId(), roomLabel(m.getRoomId()), m.getTitle(), m.getPriority(), m.getStatus(),
                    DateUtil.format(m.getReportedOn()), m.getAssignedTo(),
                    m.getTenantId() == null ? "Staff" : tenantName(m.getTenantId()));
        }
        return table.render();
    }

    public String visitors(List<VisitorLog> list) {
        TextTable table = new TextTable("ID", "Visitor", "Visiting", "Purpose", "Time in", "Time out");
        for (VisitorLog v : list) {
            table.addRow(v.getId(), v.getVisitorName(), tenantName(v.getTenantId()), v.getPurpose(),
                    DateUtil.format(v.getTimeIn()), v.isInside() ? "(inside)" : DateUtil.format(v.getTimeOut()));
        }
        return table.render();
    }

    public String users(List<User> list) {
        TextTable table = new TextTable("ID", "Username", "Full name", "Role");
        for (User u : list) {
            table.addRow(u.getId(), u.getUsername(), u.getFullName(), u.getRole());
        }
        return table.render();
    }

    public String dormitoryName(String dormitoryId) {
        try {
            return ctx.getDormitoryService().getDormitory(dormitoryId).getName();
        } catch (EntityNotFoundException e) {
            return dormitoryId;
        }
    }

    public String tenantName(String tenantId) {
        try {
            return ctx.getTenantService().getTenant(tenantId).getFullName();
        } catch (EntityNotFoundException e) {
            return tenantId;
        }
    }

    /** "Sunrise Hall 101" for a room id. */
    public String roomLabel(String roomId) {
        try {
            Room room = ctx.getRoomService().getRoom(roomId);
            return dormitoryName(room.getDormitoryId()) + " " + room.getRoomNumber();
        } catch (EntityNotFoundException e) {
            return roomId;
        }
    }
}
