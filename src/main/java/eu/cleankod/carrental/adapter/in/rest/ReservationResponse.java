package eu.cleankod.carrental.adapter.in.rest;

import eu.cleankod.carrental.domain.CarType;
import eu.cleankod.carrental.domain.Reservation;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponse(UUID id, CarType carType, LocalDateTime start, int days) {

    static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.id().value(),
                reservation.carType(),
                reservation.period().start(),
                reservation.period().days());
    }
}
