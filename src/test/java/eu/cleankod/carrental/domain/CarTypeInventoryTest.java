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
    }
}
