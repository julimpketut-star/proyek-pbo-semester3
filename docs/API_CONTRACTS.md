# API CONTRACTS — Model, Util, DAO, Service

> Status: **TERKUNCI (STEP 0)**. Signature di bawah adalah kontrak; implementasi dibuat pada STEP 3–5 (Model, DAO, Service).
> Perubahan signature wajib dicatat di `DECISION_LOG.md`. Semua uang bertipe `BigDecimal` (skala 2, `HALF_UP`).
> Semua `id` bertipe `int`. Waktu memakai `java.time.LocalDate` / `LocalTime` / `LocalDateTime`.

## 1. Model (`com.studiomusik.model`, `model.enums`)

### 1.1 Enum
```java
public enum BookingStatus { PENDING, CONFIRMED, COMPLETED, CANCELLED;
    boolean blocksSchedule();                  // true untuk PENDING, CONFIRMED (BR-07)
    boolean canTransitionTo(BookingStatus next); // tabel BR-14
}
public enum RoomStatus { AVAILABLE, MAINTENANCE }
public enum InstrumentStatus { AVAILABLE, MAINTENANCE }
```

### 1.2 `Person` (abstract) dan turunan
```java
public abstract class Person {
    private int id; private String name; private String phone; private String email;
    protected Person(int id, String name, String phone, String email);
    // getter + setter untuk keempat atribut
    public abstract String getRole();
}
public class Customer extends Person {
    private String passwordHash;
    public Customer(int id, String name, String phone, String email, String passwordHash);
    @Override public String getRole();   // "Customer"
}
public class Admin extends Person {
    private String username; private String passwordHash;
    public Admin(int id, String name, String username, String passwordHash); // phone, email = null (D-005)
    @Override public String getRole();   // "Admin"
}
```

### 1.3 Entitas lain
```java
public class Room {
    private int roomId; private String roomName; private String roomType;
    private BigDecimal pricePerHour; private RoomStatus status;
    public boolean isAvailable();                     // status == AVAILABLE
    public BigDecimal calculateCost(int hours);       // pricePerHour × hours
}
public class Instrument {
    private int instrumentId; private String name; private String type;
    private BigDecimal rentalPrice; private InstrumentStatus status;
    public boolean isAvailable();
    public BigDecimal calculateCost(int quantity);    // rentalPrice × quantity
}
public class BookingInstrument {                      // composition: bagian dari Booking
    private int bookingInstrumentId; private int bookingId;
    private Instrument instrument; private int quantity; private BigDecimal rentalPrice; // snapshot
    public BigDecimal calculateSubtotal();            // rentalPrice × quantity
}
public class Booking {
    private int bookingId; private Customer customer; private Room room;
    private LocalDate bookingDate; private LocalTime startTime; private LocalTime endTime;
    private BookingStatus status; private BigDecimal totalCost; private LocalDateTime createdAt;
    private List<BookingInstrument> instruments;      // composition
    public int calculateDuration();                   // jam bulat (endTime - startTime)
    public BigDecimal calculateTotalCost();           // room.calculateCost(durasi) + Σ subtotal
    public void addInstrument(Instrument instrument, int quantity);   // in-memory, snapshot harga
    public void removeInstrument(int instrumentId);                   // in-memory
}
```
Catatan: `checkAvailability()` dan `saveBooking()` dari Class Diagram **tidak** ada di model; lihat D-013.

## 2. Util (`com.studiomusik.util`)
```java
public final class DatabaseConnection { public static Connection getConnection() throws SQLException; } // baca config/db.properties
public final class SessionManager {                    // singleton
    public static SessionManager getInstance();
    public void login(Person user);  public void logout();
    public Person getCurrentUser();  public boolean isLoggedIn();
    public boolean isAdmin();        public boolean isCustomer();
    public Customer getCurrentCustomer();              // null bila bukan Customer
}
public final class DateTimeUtil {
    public static int hoursBetween(LocalTime start, LocalTime end);
    public static boolean isWholeHour(LocalTime t);
    public static boolean overlaps(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2); // s1 < e2 && e1 > s2
    public static String formatDate(LocalDate d); public static String formatTime(LocalTime t);
}
public final class CurrencyUtil { public static String formatRupiah(BigDecimal amount); }  // "Rp 150.000"
public final class PasswordUtil {
    public static String hash(String rawPassword);                       // pbkdf2_sha256$iter$salt$hash
    public static boolean verify(String rawPassword, String storedHash);
}
```

## 3. DAO (`com.studiomusik.dao`)
Semua method melempar `SQLException`. Method tanpa parameter `Connection` membuka dan menutup koneksi sendiri (try-with-resources). Method dengan `Connection` dipakai di dalam transaksi Service dan **tidak** menutup koneksi tersebut.

### 3.1 `AdminDAO`
```java
Optional<Admin> findByUsername(String username) throws SQLException;
Optional<Admin> findById(int adminId) throws SQLException;
```
### 3.2 `CustomerDAO`
```java
int insert(Customer customer) throws SQLException;                 // return generated customer_id
Optional<Customer> findById(int customerId) throws SQLException;
Optional<Customer> findByEmail(String email) throws SQLException;
List<Customer> findAll() throws SQLException;
boolean update(Customer customer) throws SQLException;             // name, phone, email saja
boolean delete(int customerId) throws SQLException;
boolean existsByEmail(String email) throws SQLException;
boolean existsByEmailExcludingId(String email, int customerId) throws SQLException;
long count() throws SQLException;
```
### 3.3 `RoomDAO`
```java
int insert(Room room) throws SQLException;
Optional<Room> findById(int roomId) throws SQLException;
Optional<Room> findByIdForUpdate(Connection conn, int roomId) throws SQLException;  // SELECT ... FOR UPDATE
List<Room> findAll() throws SQLException;
List<Room> findByStatus(RoomStatus status) throws SQLException;
boolean update(Room room) throws SQLException;
boolean delete(int roomId) throws SQLException;
long count() throws SQLException;
```
### 3.4 `InstrumentDAO`
```java
int insert(Instrument instrument) throws SQLException;
Optional<Instrument> findById(int instrumentId) throws SQLException;
Optional<Instrument> findById(Connection conn, int instrumentId) throws SQLException;
List<Instrument> findAll() throws SQLException;
List<Instrument> findByStatus(InstrumentStatus status) throws SQLException;
boolean update(Instrument instrument) throws SQLException;
boolean delete(int instrumentId) throws SQLException;
long count() throws SQLException;
```
### 3.5 `BookingDAO`
```java
int insert(Connection conn, Booking booking) throws SQLException;      // return generated booking_id (tanpa detail)
Optional<Booking> findById(int bookingId) throws SQLException;         // JOIN customer + room (tanpa detail alat)
Optional<Booking> findById(Connection conn, int bookingId) throws SQLException;
List<Booking> findAll() throws SQLException;                           // urut booking_date DESC, start_time DESC
List<Booking> findByCustomerId(int customerId) throws SQLException;
boolean existsOverlap(Connection conn, int roomId, LocalDate date,
                      LocalTime start, LocalTime end) throws SQLException;   // query kontrak DATABASE_CONTRACT §4
boolean existsOverlap(int roomId, LocalDate date, LocalTime start, LocalTime end) throws SQLException; // read-only UI check
boolean updateStatus(Connection conn, int bookingId, BookingStatus status) throws SQLException;
boolean updateTotalCost(Connection conn, int bookingId, BigDecimal totalCost) throws SQLException;
long countByStatus(BookingStatus status) throws SQLException;
```
### 3.6 `BookingInstrumentDAO` — CRUD relasi Many-to-Many
```java
// CREATE
int insert(Connection conn, BookingInstrument detail) throws SQLException;          // return booking_instrument_id
void insertAll(Connection conn, int bookingId, List<BookingInstrument> details) throws SQLException; // batch
// READ  (JOIN booking_instrument ↔ instrument; booking_id & instrument_id)
List<BookingInstrument> findByBookingId(int bookingId) throws SQLException;
List<BookingInstrument> findByBookingId(Connection conn, int bookingId) throws SQLException;
Optional<BookingInstrument> findById(Connection conn, int bookingInstrumentId) throws SQLException;
Optional<BookingInstrument> findByBookingIdAndInstrumentId(Connection conn, int bookingId, int instrumentId) throws SQLException;
// UPDATE  (hanya quantity; rental_price snapshot tidak berubah, BR-12)
boolean updateQuantity(Connection conn, int bookingInstrumentId, int quantity) throws SQLException;
// DELETE
boolean delete(Connection conn, int bookingInstrumentId) throws SQLException;
```

## 4. Service (`com.studiomusik.service`)
Semua method melempar `ServiceException` (unchecked; subclass `ValidationException` untuk input salah, `BusinessRuleException` untuk pelanggaran aturan bisnis). Pesan exception aman ditampilkan ke pengguna (tanpa detail SQL).

### 4.1 `AuthService`
```java
Admin    loginAdmin(String username, String rawPassword);                       // gagal → pesan generik
Customer loginCustomer(String email, String rawPassword);                       // gagal → pesan generik
Customer registerCustomer(String name, String phone, String email, String rawPassword); // validasi, email unik, hash
```
Validasi: nama/telepon/email tidak kosong, format email dasar, panjang password minimum (ditetapkan saat implementasi, dicatat di Decision Log), email belum terpakai (BR-20).

### 4.2 `CustomerService` (Admin)
```java
List<Customer> getAllCustomers();
Customer getCustomerById(int customerId);
Customer addCustomer(String name, String phone, String email, String rawPassword);
Customer updateCustomer(int customerId, String name, String phone, String email);
void deleteCustomer(int customerId);          // BR-22
long countCustomers();
```
### 4.3 `RoomService`
```java
List<Room> getAllRooms();                     // Customer & Admin (info harga)
List<Room> getBookableRooms();                // status AVAILABLE
Room getRoomById(int roomId);
Room addRoom(Room room);                      // validasi: nama/tipe tidak kosong, pricePerHour ≥ 0
Room updateRoom(Room room);
void deleteRoom(int roomId);                  // BR-22
long countRooms();
```
### 4.4 `InstrumentService`
```java
List<Instrument> getAllInstruments();
List<Instrument> getSelectableInstruments();  // status AVAILABLE (BR-06)
Instrument getInstrumentById(int instrumentId);
Instrument addInstrument(Instrument instrument);
Instrument updateInstrument(Instrument instrument);
void deleteInstrument(int instrumentId);      // BR-22
long countInstruments();
```
### 4.5 `BookingService`
```java
// Pengecekan & estimasi (read-only, untuk form Customer)
boolean isRoomAvailable(int roomId, LocalDate date, LocalTime start, LocalTime end);
        // validasi BR-02/03/04/05/21 lalu cek overlap (BR-08); melempar ValidationException bila input tidak valid
BigDecimal estimateTotalCost(int roomId, LocalTime start, LocalTime end,
                             Map<Integer, Integer> instrumentQuantities);   // key = instrumentId, value = quantity

// Pembuatan (satu transaksi, BR-15, D-019)
Booking createBooking(int customerId, int roomId, LocalDate date, LocalTime start, LocalTime end,
                      Map<Integer, Integer> instrumentQuantities);          // status awal PENDING; map boleh kosong

// Pembacaan
List<Booking> getAllBookings();                                  // Admin
List<Booking> getBookingsByCustomer(int customerId);             // Customer (riwayat miliknya)
Booking getBookingDetail(int bookingId);                         // Admin; termasuk daftar BookingInstrument
Booking getBookingForCustomer(int bookingId, int customerId);    // BR-16: tolak jika bukan miliknya
Map<BookingStatus, Long> getBookingStatusSummary();              // dashboard Admin

// Status (Admin)
void updateBookingStatus(int bookingId, BookingStatus newStatus);   // validasi BR-14

// CRUD BookingInstrument (Admin; hanya PENDING/CONFIRMED, BR-19; hitung ulang total_cost dalam transaksi)
BookingInstrument addInstrumentToBooking(int bookingId, int instrumentId, int quantity);   // CREATE (BR-06, BR-17, BR-18)
List<BookingInstrument> getBookingInstruments(int bookingId);                              // READ (JOIN)
void updateBookingInstrumentQuantity(int bookingInstrumentId, int newQuantity);            // UPDATE
void removeInstrumentFromBooking(int bookingInstrumentId);                                 // DELETE
```

## 5. Format hash password (D-018)
`pbkdf2_sha256$<iterasi>$<salt Base64>$<hash Base64>`; salt acak 16 byte (`SecureRandom`); panjang kunci 256 bit; iterasi disimpan di dalam string agar dapat dinaikkan tanpa merusak hash lama; `verify` memakai perbandingan waktu-konstan (`MessageDigest.isEqual`).

## 6. Kontrak error (pesan Indonesia untuk pengguna)
| Situasi | Exception |
|---|---|
| Input kosong/format salah, `end <= start`, durasi bukan jam bulat, tanggal lampau, quantity < 1 | `ValidationException` |
| Room/Instrument `MAINTENANCE`, jadwal bertumpuk, transisi status tidak valid, alat duplikat pada booking, booking final tidak boleh diubah, hapus entitas yang masih dipakai, email sudah terdaftar, bukan pemilik booking | `BusinessRuleException` |
| Kegagalan SQL/koneksi | `ServiceException` (pesan umum; detail teknis hanya di log developer) |
