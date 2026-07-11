package eu.cleankod.carrental.adapter.in.rest;

import eu.cleankod.carrental.application.port.in.ReserveCarUseCase;
import eu.cleankod.carrental.domain.RentalPeriod;
import eu.cleankod.carrental.domain.Reservation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReserveCarUseCase reserveCarUseCase;

    public ReservationController(ReserveCarUseCase reserveCarUseCase) {
        this.reserveCarUseCase = reserveCarUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@Valid @RequestBody ReservationRequest request) {
        RentalPeriod period = new RentalPeriod(request.start(), request.days());
        Reservation reservation = reserveCarUseCase.reserve(request.carType(), period);
        return ReservationResponse.from(reservation);
    }
}
