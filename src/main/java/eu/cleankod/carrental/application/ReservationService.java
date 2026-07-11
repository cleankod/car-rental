package eu.cleankod.carrental.application;

import eu.cleankod.carrental.application.port.in.ReserveCarUseCase;
import eu.cleankod.carrental.application.port.out.CarInventoryRepository;
import eu.cleankod.carrental.domain.CarType;
import eu.cleankod.carrental.domain.RentalPeriod;
import eu.cleankod.carrental.domain.Reservation;

import java.util.Objects;

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
