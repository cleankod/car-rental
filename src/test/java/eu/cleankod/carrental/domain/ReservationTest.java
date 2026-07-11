package eu.cleankod.carrental.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Nested
    class Of {

        @Test
        void generatesAUniqueIdForEachReservation() {
            // given
            var period = new RentalPeriod(START, 3);

            // when
            var first = Reservation.of(CarType.SEDAN, period);
            var second = Reservation.of(CarType.SEDAN, period);

            // then
            assertThat(first.id()).isNotEqualTo(second.id());
        }
    }

    @Nested
    class Overlaps {

        @Test
        void isTrueForSameCarTypeAndOverlappingPeriods() {
            // given
            var reservation = Reservation.of(CarType.SUV, new RentalPeriod(START, 5));
            var candidate = Reservation.of(CarType.SUV, new RentalPeriod(START.plusDays(2), 5));

            // when / then
            assertThat(reservation.overlaps(candidate)).isTrue();
        }

        @Test
        void isFalseForDifferentCarTypesEvenWithTheSamePeriod() {
            // given
            var period = new RentalPeriod(START, 5);
            var sedan = Reservation.of(CarType.SEDAN, period);
            var van = Reservation.of(CarType.VAN, period);

            // when / then
            assertThat(sedan.overlaps(van)).isFalse();
        }

        @Test
        void isFalseForNonOverlappingPeriodsOfTheSameCarType() {
            // given
            var first = Reservation.of(CarType.VAN, new RentalPeriod(START, 2));
            var second = Reservation.of(CarType.VAN, new RentalPeriod(START.plusDays(10), 2));

            // when / then
            assertThat(first.overlaps(second)).isFalse();
        }
    }
}
