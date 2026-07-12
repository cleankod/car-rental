package eu.cleankod.carrental.application;

import eu.cleankod.carrental.application.port.in.ReserveCarUseCase;
import eu.cleankod.carrental.application.port.out.CarInventoryRepository;
import eu.cleankod.carrental.domain.CarType;
import eu.cleankod.carrental.domain.RentalPeriod;
import eu.cleankod.carrental.domain.Reservation;

import java.util.Objects;

/**
 * Thin delegate to {@link CarInventoryRepository#reserve(CarType, RentalPeriod)} — the domain rule
 * ({@code CarTypeInventory#hasCapacityFor}) is invoked from inside the persistence adapter instead of
 * here. Checking availability and recording the reservation must be one atomic step, and only the
 * adapter holds the lock that makes that possible; splitting this into a separate find-then-save pair
 * at this layer would reopen the check-then-act race the locking exists to prevent. See
 * {@code docs/decisions/0002-use-per-car-type-locking-for-atomic-allocation.md} for the full rationale
 * and what a transactional persistence store would change.
 */
public class ReservationService implements ReserveCarUseCase {

    private final CarInventoryRepository carInventoryRepository;

    public ReservationService(CarInventoryRepository carInventoryRepository) {
        this.carInventoryRepository =
                Objects.requireNonNull(carInventoryRepository, "carInventoryRepository must not be null");
    }

    @Override
    public Reservation reserve(CarType carType, RentalPeriod period) {
        return carInventoryRepository.reserve(carType, period);
    }
}
