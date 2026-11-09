# Dorm Rental System (Java)

A console application for running a dormitory business: buildings, rooms, tenants, leases, billing,
maintenance, visitors and reports. Plain Java, no libraries, no database to install. Data is saved to
a `data/` folder automatically.

Requires **Java 8 or newer** (developed and tested on Java 21).

## Quick start

| | Windows | macOS / Linux |
|---|---|---|
| Build | `build.bat` | `./build.sh` |
| Run | `run.bat` | `./run.sh` |
| Run the tests | `test.bat` | `./test.sh` |

Or by hand, from this folder:

```
javac -d out -sourcepath src src/com/dormrental/Main.java
java -cp out com.dormrental.Main
```

**First login:** username `admin`, password `admin123` (change it under *Administration*).
To see the system with data in it, choose *Administration -> Load sample data*.

## Folder structure

```
DormRentalSystem/
├── src/com/dormrental/
│   ├── Main.java                  Entry point: login loop -> main menu
│   │
│   ├── enums/                     Fixed sets of values
│   │   ├── Amenity, DormType, Gender, RoomType, RoomStatus
│   │   ├── LeaseStatus, PaymentStatus, PaymentType, PaymentMethod
│   │   └── MaintenanceStatus, MaintenancePriority, UserRole
│   │
│   ├── model/                     The data: what the system stores
│   │   ├── Identifiable           Anything with an id
│   │   ├── Person -> Tenant       Contact details, guardian, active flag
│   │   ├── User                   Staff login account
│   │   ├── Dormitory              A building
│   │   ├── Room                   Beds, rate per bed, amenities, occupants
│   │   ├── Lease                  Tenant + room + dates + rate + deposit
│   │   ├── Payment                One bill (deposit / rent / utility / penalty)
│   │   ├── MaintenanceRequest     Repair ticket
│   │   └── VisitorLog             Front-desk sign-in / sign-out
│   │
│   ├── repository/                Storage
│   │   ├── Repository             Interface: save, find, delete, nextId ...
│   │   └── FileRepository         Saves each entity type to data/<name>.dat
│   │
│   ├── service/                   Business rules (no printing, no typing)
│   │   ├── AuthService            Login, accounts, password change
│   │   ├── DormitoryService       Buildings
│   │   ├── RoomService            Rooms, search, maintenance mode, beds
│   │   ├── TenantService          Registration, search, (de)activation
│   │   ├── LeaseService           Move-in, renew, terminate, auto-expire
│   │   ├── PaymentService         Billing schedule, payments, overdue, late fees
│   │   ├── MaintenanceService     Tickets ordered by urgency
│   │   ├── VisitorService         Check in / out
│   │   └── ReportService          Occupancy, revenue, overdue, ledger, receipt
│   │
│   ├── ui/                        Everything the user sees and types
│   │   ├── Menu                   Base class for every menu screen
│   │   ├── LoginScreen, MainMenu
│   │   ├── DormitoryMenu, RoomMenu, TenantMenu, LeaseMenu, PaymentMenu
│   │   ├── MaintenanceMenu, VisitorMenu, ReportMenu, AdminMenu
│   │   ├── InputHelper            Validated keyboard input
│   │   ├── Display                Headers, [OK] / [!] messages
│   │   └── Tables                 Lists of objects -> printable tables
│   │
│   ├── config/
│   │   ├── AppContext             Creates repositories and services, wires them together
│   │   └── DataSeeder             Demo data
│   │
│   ├── exception/                 Specific, readable errors
│   │   └── DormRentalException (base), EntityNotFound, Validation,
│   │       DuplicateEntity, BusinessRule, Authentication, InputClosed
│   │
│   └── util/                      DateUtil, MoneyUtil, PasswordUtil, Validator, TextTable
│
├── test/com/dormrental/ServiceSmokeTest.java    42 checks of the business rules
├── data/                          Created on first run (your saved data)
├── build.bat / build.sh   run.bat / run.sh   test.bat / test.sh
└── README.md
```

How the layers talk: `ui -> service -> repository -> file`. The UI never touches files, and services never
print, so you could swap the console for a GUI or a web front end without rewriting the rules.

## What it does

- **Dormitories**: male-only, female-only or co-ed buildings.
- **Rooms**: single / double / triple / quad. The rate is **per bed**. Amenities, search by type and budget,
  maintenance mode (blocks new tenants).
- **Tenants**: register with guardian details, search, deactivate / reactivate, profile with current room and balance.
- **Leases**: move-in, renew, terminate early, automatic closing when the end date passes.
- **Payments**: a deposit bill plus one rent bill per month are generated for every lease. Record payments
  (cash, GCash, bank transfer, card), add utility or penalty charges, print receipts, tenant ledger.
- **Maintenance**: tickets with priority, assignment to staff, status updates, most urgent first.
- **Visitors**: check in / out, who is inside right now, history per tenant.
- **Reports**: occupancy by dormitory, monthly revenue (billed vs collected), overdue payments, tenant ledger.
- **Accounts**: admin and staff roles, salted password hashing.

## Rules the system enforces

- A tenant can only live in a dormitory that accepts their gender.
- A tenant can only have one active lease at a time, and deactivated tenants cannot lease.
- A room cannot be over capacity, and a room under maintenance cannot take new tenants.
- Lease length is 1-24 months. The lease's last day is the day before the same date N months later.
- Unpaid bills past their due date become OVERDUE, and each overdue **rent** bill gets one 5% late fee.
  This runs at every login and from *Administration -> Run daily tasks now*.
- Terminating a lease frees the bed and cancels unpaid rent bills after the effective date.
- A dormitory cannot be deleted while it has rooms, a room cannot be deleted while someone lives in it.

## Design notes

- Money uses `BigDecimal`, never `double`, so cents never drift.
- Saved data uses Java serialization (one `.dat` file per entity type). That keeps the project dependency-free,
  but the files are only readable by this program and may stop loading if you change a model class.
  If that happens, delete the `data/` folder. A natural next step is replacing `FileRepository` with a JDBC
  repository; nothing else needs to change because services only know the `Repository` interface.
- Passwords are salted SHA-256. That is fine for a learning project; a production system should use bcrypt or Argon2.
- The password is typed in plain view in the console (Java's `Scanner` cannot hide input).

## Ideas for extending it

Dormitory-wide announcements, utility meter readings that generate bills automatically, security deposit refunds on
move-out, room transfer between rooms, receipts exported to PDF, a JavaFX or web front end.
