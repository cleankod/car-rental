package eu.cleankod.carrental.application;

import eu.cleankod.carrental.adapter.out.persistence.InMemoryCarInventoryRepository;
import eu.cleankod.carrental.application.port.in.ReserveCarUseCase;
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

/**
 * Exercises {@link ReservationService} through the {@link ReserveCarUseCase} endpoint against the real
 * in-memory repository. Detailed capacity/overlap behaviour is covered by
 * {@code InMemoryCarInventoryRepositoryTest} and {@code CarTypeInventoryTest} — these tests only need to
 * confirm the use case correctly wires a request through to the port and back.
 */
class ReservationServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Nested
    class Reserve {

        @Test
        void returnsAReservationWhenAUnitOfTheRequestedTypeIsAvailable() {
            // given
            ReserveCarUseCase useCase = new ReservationService(
                    new InMemoryCarInventoryRepository(new CarTypeInventory(CarType.SEDAN, 1)));
            RentalPeriod period = new RentalPeriod(START, 3);

            // when
            Reservation reservation = useCase.reserve(CarType.SEDAN, period);

            // then
            assertThat(reservation.carType()).isEqualTo(CarType.SEDAN);
            assertThat(reservation.period()).isEqualTo(period);
        }

        @Test
        void isRejectedWhenNoUnitOfThatTypeRemainsAvailable() {
            // given
            ReserveCarUseCase useCase = new ReservationService(
                    new InMemoryCarInventoryRepository(new CarTypeInventory(CarType.SUV, 1)));
            RentalPeriod period = new RentalPeriod(START, 3);
            useCase.reserve(CarType.SUV, period);

            // when
            var thrown = catchThrowable(() -> useCase.reserve(CarType.SUV, new RentalPeriod(START, 3)));

            // then
            assertThat(thrown).isInstanceOf(CarUnavailableException.class)
                    .hasMessage("no unit of carType SUV is available for period " + period);
        }
    }
}
