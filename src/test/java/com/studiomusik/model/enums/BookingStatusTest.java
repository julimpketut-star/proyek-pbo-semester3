package com.studiomusik.model.enums;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** BR-07 (blocksSchedule) dan BR-14 (semua kombinasi transisi), serta nama enum = nilai ENUM database. */
class BookingStatusTest {

    private static final BookingStatus P = BookingStatus.PENDING;
    private static final BookingStatus CF = BookingStatus.CONFIRMED;
    private static final BookingStatus CP = BookingStatus.COMPLETED;
    private static final BookingStatus CN = BookingStatus.CANCELLED;

    @Test
    void blocksScheduleOnlyForPendingAndConfirmed() {
        assertTrue(P.blocksSchedule());
        assertTrue(CF.blocksSchedule());
        assertFalse(CP.blocksSchedule());
        assertFalse(CN.blocksSchedule());
    }

    @Test
    void allSixteenTransitionCombinationsFollowBr14() {
        // baris = status asal (urutan P, CF, CP, CN); kolom = status tujuan (urutan sama)
        BookingStatus[] order = {P, CF, CP, CN};
        boolean[][] expected = {
                /* PENDING   -> */ {false, true,  false, true},
                /* CONFIRMED -> */ {false, false, true,  true},
                /* COMPLETED -> */ {false, false, false, false},
                /* CANCELLED -> */ {false, false, false, false},
        };
        List<Executable> checks = new ArrayList<>();
        for (int from = 0; from < order.length; from++) {
            for (int to = 0; to < order.length; to++) {
                BookingStatus a = order[from];
                BookingStatus b = order[to];
                boolean want = expected[from][to];
                checks.add(() -> assertEquals(want, a.canTransitionTo(b), a + " -> " + b));
            }
        }
        assertEquals(16, checks.size());
        assertAll(checks);
    }

    @Test
    void transitionToNullIsRejected() {
        for (BookingStatus status : BookingStatus.values()) {
            assertFalse(status.canTransitionTo(null), status + " -> null");
        }
    }

    @Test
    void finalStatusesHaveNoOutgoingTransition() {
        for (BookingStatus next : BookingStatus.values()) {
            assertFalse(CP.canTransitionTo(next));
            assertFalse(CN.canTransitionTo(next));
        }
    }

    @Test
    void enumNamesMatchDatabaseEnumValuesAndOrder() {
        assertEquals(List.of("PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"),
                java.util.Arrays.stream(BookingStatus.values()).map(Enum::name).toList());
        assertEquals(List.of("AVAILABLE", "MAINTENANCE"),
                java.util.Arrays.stream(RoomStatus.values()).map(Enum::name).toList());
        assertEquals(List.of("AVAILABLE", "MAINTENANCE"),
                java.util.Arrays.stream(InstrumentStatus.values()).map(Enum::name).toList());
    }

    @Test
    void valueOfRejectsUnknownStatusLikeTheDatabaseDoes() {
        assertEquals(BookingStatus.CONFIRMED, BookingStatus.valueOf("CONFIRMED"));
        assertThrows(IllegalArgumentException.class, () -> BookingStatus.valueOf("BOGUS"));
        assertThrows(IllegalArgumentException.class, () -> RoomStatus.valueOf("BOGUS"));
        assertThrows(IllegalArgumentException.class, () -> InstrumentStatus.valueOf("BOGUS"));
    }
}
