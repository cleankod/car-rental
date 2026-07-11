package eu.cleankod.carrental.application.port.out;

import eu.cleankod.carrental.domain.CarType;
import eu.cleankod.carrental.domain.CarUnavailableException;
import eu.cleankod.carrental.domain.RentalPeriod;
import eu.cleankod.carrental.domain.Reservation;

/**
 * Atomically attempts to reserve one unit of a {@link CarType} for a {@link RentalPeriod}: checking
 * availability and recording the reservation are a single indivisible step from the caller's
 * perspective. This is the boundary that hides the concurrency-control mechanism — how that atomicity
 * is actually achieved — from the application and domain layers.
 */
public interface CarInventoryRepository {

    /**
     * @throws CarUnavailableException if no unit of {@code carType} is free for the whole {@code period}
     */
    Reservation reserve(CarType carType, RentalPeriod period);
}
