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
 * <p><strong>Not yet concurrency-safe.</strong> Two overlapping {@code reserve} calls racing for the
 * last available unit of a type can both read "capacity available" before either records its
 * reservation. Making the check-and-record step atomic is the {@code in-memory-persistence} stage's
 * job, not this one.
 */
public class InMemoryCarInventoryRepository implements CarInventoryRepository {

    private final Map<CarType, CarTypeInventory> inventoriesByType = new EnumMap<>(CarType.class);
    private final List<Reservation> reservations = new ArrayList<>();

    public InMemoryCarInventoryRepository(CarTypeInventory... inventories) {
        for (CarTypeInventory inventory : inventories) {
            inventoriesByType.put(inventory.carType(), inventory);
        }
    }

    @Override
    public Reservation reserve(CarType carType, RentalPeriod period) {
        CarTypeInventory inventory = inventoriesByType.get(carType);
        List<RentalPeriod> existingPeriods = reservations.stream()
                .filter(reservation -> reservation.carType() == carType)
                .map(Reservation::period)
                .toList();
        if (!inventory.hasCapacityFor(existingPeriods, period)) {
            throw new CarUnavailableException(carType, period);
        }
        Reservation reservation = Reservation.of(carType, period);
        reservations.add(reservation);
        return reservation;
    }
}
