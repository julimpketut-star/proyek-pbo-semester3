package com.studiomusik.service;

import com.studiomusik.model.Customer;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.RoomStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.studiomusik.service.ServiceTestSupport.TEST_PASSWORD;
import static com.studiomusik.service.ServiceTestSupport.futureDate;
import static com.studiomusik.service.ServiceTestSupport.t;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tes integrasi CustomerService (CRUD Admin, email unik, BR-22). */
class CustomerServiceTest {

    private final CustomerService customers = new CustomerService();

    @BeforeAll
    static void requireDb() {
        ServiceTestSupport.requireDatabaseWithSeed();
        ServiceTestSupport.cleanUp();
    }

    @AfterEach
    void cleanUp() {
        ServiceTestSupport.cleanUp();
    }

    @Test
    @DisplayName("Create/Read: addCustomer, getCustomerById, getAllCustomers, countCustomers; tanpa hash")
    void createAndRead() {
        long before = customers.countCustomers();
        String email = ServiceTestSupport.uniqueEmail();

        Customer created = customers.addCustomer("  Made Uji  ", "0812 3456 7890", email, TEST_PASSWORD);
        assertTrue(created.getId() > 0);
        assertEquals("Made Uji", created.getName());
        assertNull(created.getPasswordHash());

        Customer read = customers.getCustomerById(created.getId());
        assertEquals(email, read.getEmail());
        assertEquals("081234567890", read.getPhone());
        assertNull(read.getPasswordHash());

        assertEquals(before + 1, customers.countCustomers());
        List<Customer> all = customers.getAllCustomers();
        assertTrue(all.stream().anyMatch(c -> c.getId() == created.getId()));
        assertTrue(all.stream().allMatch(c -> c.getPasswordHash() == null), "hash tidak menyebar ke daftar");
    }

    @Test
    @DisplayName("addCustomer: email duplikat ditolak (BR-20)")
    void addDuplicateEmail() {
        String email = ServiceTestSupport.uniqueEmail();
        customers.addCustomer("Satu", "081234567890", email, TEST_PASSWORD);
        assertThrows(BusinessRuleException.class,
                () -> customers.addCustomer("Dua", "081234567891", email, TEST_PASSWORD));
    }

    @Test
    @DisplayName("Update: name/phone/email berubah; email milik sendiri boleh tetap; email Customer lain ditolak")
    void update() throws Exception {
        Customer a = ServiceTestSupport.newCustomer();
        Customer b = ServiceTestSupport.newCustomer();
        String newEmail = ServiceTestSupport.uniqueEmail();

        Customer updated = customers.updateCustomer(a.getId(), "Nama Baru", "+62 812-0000-1111", newEmail);
        assertEquals("Nama Baru", updated.getName());
        assertEquals("+6281200001111", updated.getPhone());
        assertEquals(newEmail, customers.getCustomerById(a.getId()).getEmail());

        // email sendiri tidak dianggap duplikat
        customers.updateCustomer(a.getId(), "Nama Baru 2", "081200001111", newEmail);
        assertEquals("Nama Baru 2", customers.getCustomerById(a.getId()).getName());

        // email Customer lain ditolak
        assertThrows(BusinessRuleException.class,
                () -> customers.updateCustomer(a.getId(), "X", "081200001111", b.getEmail()));
        assertEquals(newEmail, customers.getCustomerById(a.getId()).getEmail(), "data tidak berubah");

        assertThrows(ValidationException.class,
                () -> customers.updateCustomer(999_999_999, "X", "081200001111", ServiceTestSupport.uniqueEmail()));
    }

    @Test
    @DisplayName("Update tidak mengubah password (hash tetap dapat dipakai login)")
    void updateKeepsPassword() throws Exception {
        Customer a = ServiceTestSupport.newCustomer();
        String newEmail = ServiceTestSupport.uniqueEmail();
        customers.updateCustomer(a.getId(), "Nama Baru", "081200001111", newEmail);
        assertEquals(newEmail, new AuthService().loginCustomer(newEmail, TEST_PASSWORD).getEmail());
    }

    @Test
    @DisplayName("Read/Delete: id tidak ada -> ValidationException; hapus Customer tanpa booking berhasil")
    void deleteWithoutBooking() throws Exception {
        assertThrows(ValidationException.class, () -> customers.getCustomerById(999_999_999));
        assertThrows(ValidationException.class, () -> customers.deleteCustomer(999_999_999));

        Customer a = ServiceTestSupport.newCustomer();
        customers.deleteCustomer(a.getId());
        assertThrows(ValidationException.class, () -> customers.getCustomerById(a.getId()));
    }

    @Test
    @DisplayName("AC-R19: hapus Customer yang punya Booking ditolak dengan pesan jelas; data tetap")
    void deleteCustomerWithBookingIsRejected() throws Exception {
        Customer a = ServiceTestSupport.newCustomer();
        Room room = ServiceTestSupport.newRoom("100000.00", RoomStatus.AVAILABLE);
        new BookingService().createBooking(a.getId(), room.getRoomId(), futureDate(30), t(10), t(12), Map.of());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> customers.deleteCustomer(a.getId()));
        assertTrue(ex.getMessage().contains("booking"), ex.getMessage());
        assertEquals(a.getEmail(), customers.getCustomerById(a.getId()).getEmail(), "Customer masih ada");
    }

    @Test
    @DisplayName("AC-R19: Customer seed yang punya booking (id 1) tidak dapat dihapus")
    void seedCustomerWithBookingCannotBeDeleted() {
        assertThrows(BusinessRuleException.class, () -> customers.deleteCustomer(1));
        assertEquals("ayu.lestari@example.com", customers.getCustomerById(1).getEmail());
    }
}
