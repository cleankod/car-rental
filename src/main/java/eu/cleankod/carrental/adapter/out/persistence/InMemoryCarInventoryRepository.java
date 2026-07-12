package eu.cleankod.carrental.adapter.out.persistence;

import eu.cleankod.carrental.application.port.out.CarInventoryRepository;
import eu.cleankod.carrental.domain.CarType;
import eu.cleankod.carrental.domain.CarTypeInventory;
import eu.cleankod.carrental.domain.CarUnavailableException;
import eu.cleankod.carrental.domain.RentalPeriod;
import eu.cleankod.carrental.domain.Reservation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link CarInventoryRepository}: holds a fixed {@link CarTypeInventory} per {@link CarType}
 * and the reservations accepted so far, and applies {@link CarTypeInventory#hasCapacityFor} to decide
 * whether a new reservation fits.
 *
 * <p>Concurrency-safe per car type: {@code reserve} synchronizes on a dedicated lock object for the
 * requested {@link CarType}, so the whole check-then-record step is atomic for that type — concurrent
 * attempts for the same type are serialized, but attempts for different types never contend with each
 * other, since both the reservation lists and the locks are partitioned per type. See
 * {@code docs/decisions/0002-use-per-car-type-locking-for-atomic-allocation.md} for the alternatives
 * considered and why.
 */
public class InMemoryCarInventoryRepository implements CarInventoryRepository {

    private final Map<CarType, CarTypeInventory> inventoriesByType = new EnumMap<>(CarType.class);
    private final Map<CarType, List<Reservation>> reservationsByType = new EnumMap<>(CarType.class);
    private final Map<CarType, Object> locksByType = new EnumMap<>(CarType.class);

    public InMemoryCarInventoryRepository(CarTypeInventory... inventories) {
        for (CarTypeInventory inventory : inventories) {
            CarType carType = inventory.carType();
            inventoriesByType.put(carType, inventory);
            reservationsByType.put(carType, new ArrayList<>());
            locksByType.put(carType, new Object());
        }
    }

    @Override
    public Reservation reserve(CarType carType, RentalPeriod period) {
        synchronized (locksByType.get(carType)) {
            CarTypeInventory inventory = inventoriesByType.get(carType);
            List<Reservation> existingReservations = reservationsByType.get(carType);
            Reservation candidate = Reservation.of(carType, period);
            if (!inventory.hasCapacityFor(existingReservations, candidate)) {
                throw new CarUnavailableException(carType, period);
            }
            existingReservations.add(candidate);
            return candidate;
        }
    }
}
