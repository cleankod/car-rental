package eu.cleankod.carrental.application.port.in;

import eu.cleankod.carrental.domain.CarType;
import eu.cleankod.carrental.domain.CarUnavailableException;
import eu.cleankod.carrental.domain.RentalPeriod;
import eu.cleankod.carrental.domain.Reservation;

/**
 * Reserve one unit of a given {@link CarType} for a {@link RentalPeriod}.
 */
public interface ReserveCarUseCase {

    /**
     * @throws CarUnavailableException if no unit of {@code carType} is free for the whole {@code period}
     */
    Reservation reserve(CarType carType, RentalPeriod period);
}
