package eu.cleankod.carrental.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CarTypeInventoryTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Nested
    class Constructor {

        @Test
        void rejectsNonPositiveTotalUnits() {
            // given / when
            var thrown = catchThrowable(() -> new CarTypeInventory(CarType.SEDAN, 0));

            // then
            assertThat(thrown).isInstanceOf(InvalidFleetSizeException.class)
                    .hasMessage("totalUnits must be positive for carType SEDAN, was: 0");
        }
    }

    @Nested
    class HasCapacityFor {

        @Test
        void isTrueWhenNoExistingReservationsOverlap() {
            // given
            var inventory = new CarTypeInventory(CarType.SEDAN, 1);
            var candidate = Reservation.of(CarType.SEDAN, new RentalPeriod(START, 3));

            // when
            var hasCapacity = inventory.hasCapacityFor(List.of(), candidate);

            // then
            assertThat(hasCapacity).isTrue();
        }

        @Test
        void isFalseWhenTheSingleUnitIsAlreadyReservedForAnOverlappingPeriod() {
            // given
            var inventory = new CarTypeInventory(CarType.SEDAN, 1);
            var existing = Reservation.of(CarType.SEDAN, new RentalPeriod(START, 3));
            var candidate = Reservation.of(CarType.SEDAN, new RentalPeriod(START.plusDays(1), 3));

            // when
            var hasCapacity = inventory.hasCapacityFor(List.of(existing), candidate);

            // then
            assertThat(hasCapacity).isFalse();
        }

        @Test
        void isTrueWhenASecondUnitIsStillFree() {
            // given
            var inventory = new CarTypeInventory(CarType.SUV, 2);
            var existing = Reservation.of(CarType.SUV, new RentalPeriod(START, 3));
            var candidate = Reservation.of(CarType.SUV, new RentalPeriod(START.plusDays(1), 3));

            // when
            var hasCapacity = inventory.hasCapacityFor(List.of(existing), candidate);

            // then
            assertThat(hasCapacity).isTrue();
        }

        @Test
        void isFalseWhenAllUnitsAreAlreadyReservedForOverlappingPeriods() {
            // given
            var inventory = new CarTypeInventory(CarType.VAN, 2);
            var firstExisting = Reservation.of(CarType.VAN, new RentalPeriod(START, 5));
            var secondExisting = Reservation.of(CarType.VAN, new RentalPeriod(START.plusDays(1), 5));
            var candidate = Reservation.of(CarType.VAN, new RentalPeriod(START.plusDays(2), 5));

            // when
            var hasCapacity = inventory.hasCapacityFor(List.of(firstExisting, secondExisting), candidate);

            // then
            assertThat(hasCapacity).isFalse();
        }

        @Test
        void ignoresExistingReservationsThatDoNotOverlapTheCandidate() {
            // given
            var inventory = new CarTypeInventory(CarType.SEDAN, 1);
            var existing = Reservation.of(CarType.SEDAN, new RentalPeriod(START, 2));
            var candidate = Reservation.of(CarType.SEDAN, new RentalPeriod(START.plusDays(10), 2));

            // when
            var hasCapacity = inventory.hasCapacityFor(List.of(existing), candidate);

            // then
            assertThat(hasCapacity).isTrue();
        }

        @Test
        void isTrueWhenExistingReservationsOverlapTheCandidateButNotEachOther() {
            // given: firstExisting and secondExisting don't overlap each other, so one vehicle could
            // serve both sequentially, freeing the second vehicle for the candidate — even though the
            // candidate overlaps both of them individually
            var inventory = new CarTypeInventory(CarType.SUV, 2);
            var firstExisting = Reservation.of(CarType.SUV, new RentalPeriod(START, 5));
            var secondExisting = Reservation.of(CarType.SUV, new RentalPeriod(START.plusDays(10), 5));
            var candidate = Reservation.of(CarType.SUV, new RentalPeriod(START.plusDays(3), 9));

            // when
            var hasCapacity = inventory.hasCapacityFor(List.of(firstExisting, secondExisting), candidate);

            // then
            assertThat(hasCapacity).isTrue();
        }

        @Test
        void isTrueWhenCandidateStartsExactlyWhenTheOnlyExistingReservationEnds() {
            // given: the sweep must process the existing reservation's end (-1) before the candidate's
            // start (+1) at the exact same instant, so a back-to-back booking is never counted as
            // simultaneous — this protects the sweep's own tie-breaking directly, independent of the
            // RentalPeriod.overlaps-based proof of the same adjacency rule
            var inventory = new CarTypeInventory(CarType.SEDAN, 1);
            var existing = Reservation.of(CarType.SEDAN, new RentalPeriod(START, 2));
            var candidate = Reservation.of(CarType.SEDAN, new RentalPeriod(existing.period().endExclusive(), 2));

            // when
            var hasCapacity = inventory.hasCapacityFor(List.of(existing), candidate);

            // then
            assertThat(hasCapacity).isTrue();
        }

        @Test
        void isTrueWhenMultipleReservationsStartAndEndAtTheSameInstant() {
            // given: two reservations share the same start instant, and two more (one existing, one
            // the candidate) share the exact instant the first two end. A sweep that processes ends
            // before starts at a tied instant never sees more than 2 simultaneously, even though four
            // start/end events land on only two timestamps
            var inventory = new CarTypeInventory(CarType.VAN, 2);
            var firstEarlyExisting = Reservation.of(CarType.VAN, new RentalPeriod(START, 2));
            var secondEarlyExisting = Reservation.of(CarType.VAN, new RentalPeriod(START, 2));
            var lateExisting = Reservation.of(CarType.VAN, new RentalPeriod(START.plusDays(2), 2));
            var candidate = Reservation.of(CarType.VAN, new RentalPeriod(START.plusDays(2), 2));

            // when
            var hasCapacity = inventory.hasCapacityFor(
                    List.of(firstEarlyExisting, secondEarlyExisting, lateExisting), candidate);

            // then
            assertThat(hasCapacity).isTrue();
        }
    }
}
