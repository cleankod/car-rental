package eu.cleankod.carrental.adapter.out.persistence;

import eu.cleankod.carrental.domain.CarType;
import eu.cleankod.carrental.domain.CarTypeInventory;
import eu.cleankod.carrental.domain.CarUnavailableException;
import eu.cleankod.carrental.domain.RentalPeriod;
import eu.cleankod.carrental.domain.Reservation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class InMemoryCarInventoryRepositoryTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Nested
    class Reserve {

        @Test
        void succeedsWhenAUnitOfTheRequestedTypeIsAvailableForTheWholePeriod() {
            // given
            InMemoryCarInventoryRepository repository =
                    new InMemoryCarInventoryRepository(new CarTypeInventory(CarType.SEDAN, 1));
            RentalPeriod period = new RentalPeriod(START, 3);

            // when
            Reservation reservation = repository.reserve(CarType.SEDAN, period);

            // then
            assertThat(reservation.carType()).isEqualTo(CarType.SEDAN);
            assertThat(reservation.period()).isEqualTo(period);
        }

        @Test
        void isRejectedWhenNoUnitOfThatTypeRemainsAvailable() {
            // given
            InMemoryCarInventoryRepository repository =
                    new InMemoryCarInventoryRepository(new CarTypeInventory(CarType.SUV, 1));
            RentalPeriod period = new RentalPeriod(START, 3);
            repository.reserve(CarType.SUV, period);

            // when
            var thrown = catchThrowable(() -> repository.reserve(CarType.SUV, new RentalPeriod(START, 3)));

            // then
            assertThat(thrown).isInstanceOf(CarUnavailableException.class)
                    .hasMessage("no unit of carType SUV is available for period " + period);
        }

        @Test
        void isRejectedWhenTheRequestedPeriodOverlapsAnAlreadyStoredReservationAndNoUnitIsLeft() {
            // given
            InMemoryCarInventoryRepository repository =
                    new InMemoryCarInventoryRepository(new CarTypeInventory(CarType.VAN, 1));
            repository.reserve(CarType.VAN, new RentalPeriod(START, 5));

            // when
            var overlapping = new RentalPeriod(START.plusDays(2), 5);
            var thrown = catchThrowable(() -> repository.reserve(CarType.VAN, overlapping));

            // then
            assertThat(thrown).isInstanceOf(CarUnavailableException.class);
        }

        @Test
        void succeedsForANonOverlappingPeriodAfterAnAlreadyStoredReservationOfTheSameType() {
            // given
            InMemoryCarInventoryRepository repository =
                    new InMemoryCarInventoryRepository(new CarTypeInventory(CarType.VAN, 1));
            RentalPeriod firstPeriod = new RentalPeriod(START, 3);
            repository.reserve(CarType.VAN, firstPeriod);

            // when
            RentalPeriod secondPeriod = new RentalPeriod(firstPeriod.endExclusive(), 2);
            Reservation reservation = repository.reserve(CarType.VAN, secondPeriod);

            // then
            assertThat(reservation.period()).isEqualTo(secondPeriod);
        }

        @Test
        void tracksCapacityIndependentlyPerCarType() {
            // given
            InMemoryCarInventoryRepository repository = new InMemoryCarInventoryRepository(
                    new CarTypeInventory(CarType.SEDAN, 1), new CarTypeInventory(CarType.VAN, 1));
            RentalPeriod period = new RentalPeriod(START, 3);
            repository.reserve(CarType.SEDAN, period);

            // when: a VAN reservation for the exact same period should not be blocked by the SEDAN one
            Reservation reservation = repository.reserve(CarType.VAN, period);

            // then
            assertThat(reservation.carType()).isEqualTo(CarType.VAN);
        }
    }
}
