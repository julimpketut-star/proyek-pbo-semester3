package com.studiomusik.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.studiomusik.model.enums.BookingStatus;
import com.studiomusik.model.enums.InstrumentStatus;
import com.studiomusik.model.enums.RoomStatus;

/**
 * AC-M05 — bukti perilaku (bukan sekadar nama/komentar) bahwa tujuh konsep PBO benar-benar
 * diterapkan pada class dan method nyata. Tabel pemetaannya ada di docs/ACCEPTANCE_CRITERIA.md.
 */
class OopConceptsTest {

    private static final List<Class<?>> MODEL_CLASSES = List.of(
            Person.class, Customer.class, Admin.class, Room.class, Instrument.class,
            BookingInstrument.class, Booking.class);

    // 1. Encapsulation ------------------------------------------------------------------

    @Test
    void encapsulation_everyInstanceFieldOfEveryModelClassIsPrivate() {
        for (Class<?> type : MODEL_CLASSES) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue; // konstanta/ROLE statis bukan state objek
                }
                assertTrue(Modifier.isPrivate(field.getModifiers()),
                        type.getSimpleName() + "." + field.getName() + " harus private");
            }
        }
    }

    @Test
    void encapsulation_stateIsReachedOnlyThroughMethodsThatEnforceInvariants() {
        BookingInstrument detail = new BookingInstrument(0, 1, Fixtures.instrument(2, "Gitar", "25000"), 2, BigDecimal.TEN);
        assertThrows(IllegalArgumentException.class, () -> detail.setQuantity(0)); // invariant dijaga setter
        assertEquals(2, detail.getQuantity());

        Booking booking = new Booking(Fixtures.customer(), Fixtures.room("1"), LocalDate.of(2026, 12, 1),
                LocalTime.of(10, 0), LocalTime.of(11, 0));
        assertThrows(UnsupportedOperationException.class, () -> booking.getInstruments().clear()); // list internal terlindungi
    }

    // 2. Abstraction --------------------------------------------------------------------

    @Test
    void abstraction_personIsAbstractClassWithAbstractOperation() throws Exception {
        assertTrue(Modifier.isAbstract(Person.class.getModifiers()));
        assertTrue(Modifier.isAbstract(Person.class.getDeclaredMethod("getRole").getModifiers()));
    }

    // 3. Inheritance --------------------------------------------------------------------

    @Test
    void inheritance_customerAndAdminExtendPersonAndReuseItsState() {
        assertSame(Person.class, Customer.class.getSuperclass());
        assertSame(Person.class, Admin.class.getSuperclass());
        // method milik Person dipakai langsung pada subclass tanpa didefinisikan ulang
        Method getName = null;
        try {
            getName = Customer.class.getMethod("getName");
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        assertSame(Person.class, getName.getDeclaringClass());
        assertEquals("Made Ayu Lestari", Fixtures.customer().getName());
    }

    // 4. Polymorphism -------------------------------------------------------------------

    @Test
    void polymorphism_sameCallOnPersonReferenceGivesRoleSpecificResult() throws Exception {
        List<Person> users = List.of(Fixtures.customer(), new Admin(1, "Admin", "admin", "h"));
        assertEquals(List.of("Customer", "Admin"), users.stream().map(Person::getRole).toList());
        // override nyata: method dideklarasikan ulang di subclass, bukan diwarisi
        assertSame(Customer.class, Customer.class.getDeclaredMethod("getRole").getDeclaringClass());
        assertSame(Admin.class, Admin.class.getDeclaredMethod("getRole").getDeclaringClass());
    }

    // 5. Association --------------------------------------------------------------------

    @Test
    void association_bookingReferencesItsCustomerAndRoomAndOneCustomerHasManyBookings() {
        Customer customer = Fixtures.customer();
        Room room = Fixtures.room("100000");
        Booking first = new Booking(customer, room, LocalDate.of(2026, 12, 1), LocalTime.of(10, 0), LocalTime.of(12, 0));
        Booking second = new Booking(customer, room, LocalDate.of(2026, 12, 2), LocalTime.of(10, 0), LocalTime.of(12, 0));
        assertSame(customer, first.getCustomer());
        assertSame(room, first.getRoom());
        assertSame(first.getCustomer(), second.getCustomer()); // 1 Customer : N Booking
        assertSame(first.getRoom(), second.getRoom());         // 1 Room : N Booking
    }

    @Test
    void association_bookingInstrumentReferencesInstrumentManyToMany() {
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        Booking a = new Booking(Fixtures.customer(), Fixtures.room("1"), LocalDate.of(2026, 12, 1), LocalTime.of(10, 0), LocalTime.of(11, 0));
        Booking b = new Booking(Fixtures.customer(), Fixtures.room("1"), LocalDate.of(2026, 12, 2), LocalTime.of(10, 0), LocalTime.of(11, 0));
        a.addInstrument(guitar, 1);
        b.addInstrument(guitar, 2);                             // satu alat dipakai banyak booking
        assertSame(guitar, a.getInstruments().get(0).getInstrument());
        assertSame(guitar, b.getInstruments().get(0).getInstrument());

        Instrument bass = Fixtures.instrument(3, "Bass", "25000");
        a.addInstrument(bass, 1);                               // satu booking memakai banyak alat
        assertEquals(2, a.getInstruments().size());
    }

    // 6. Composition --------------------------------------------------------------------

    @Test
    void composition_bookingCreatesAndOwnsItsDetailsAndTheyFollowItsIdentity() {
        Booking booking = new Booking(Fixtures.customer(), Fixtures.room("100000"), LocalDate.of(2026, 12, 1),
                LocalTime.of(10, 0), LocalTime.of(12, 0));
        booking.addInstrument(Fixtures.instrument(2, "Gitar", "25000"), 2); // detail dibuat oleh Booking
        BookingInstrument detail = booking.getInstruments().get(0);

        // pihak luar tidak dapat menyisipkan/mengambil detail lewat daftar internal
        assertThrows(UnsupportedOperationException.class, () -> booking.getInstruments().add(detail));
        assertThrows(UnsupportedOperationException.class, () -> booking.getInstruments().remove(detail));

        booking.setBookingId(7);                                // identitas induk mengalir ke bagian
        assertEquals(7, detail.getBookingId());

        // total induk dihitung dari bagian-bagiannya; bagian dibuang bersama induknya
        assertEquals(new BigDecimal("250000.00"), booking.calculateTotalCost());
        booking.setInstruments(List.of());
        assertEquals(new BigDecimal("200000.00"), booking.calculateTotalCost());
    }

    // 7. Enumeration --------------------------------------------------------------------

    @Test
    void enumeration_statusFieldsAreEnumsAndBookingStatusCarriesBehaviour() throws Exception {
        assertTrue(BookingStatus.class.isEnum());
        assertTrue(RoomStatus.class.isEnum());
        assertTrue(InstrumentStatus.class.isEnum());

        assertSame(BookingStatus.class, Booking.class.getMethod("getStatus").getReturnType());
        assertSame(RoomStatus.class, Room.class.getMethod("getStatus").getReturnType());
        assertSame(InstrumentStatus.class, Instrument.class.getMethod("getStatus").getReturnType());

        assertTrue(BookingStatus.PENDING.blocksSchedule());
        assertFalse(BookingStatus.CANCELLED.blocksSchedule());
        assertTrue(BookingStatus.PENDING.canTransitionTo(BookingStatus.CONFIRMED));
    }
}
