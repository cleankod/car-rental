package eu.cleankod.carrental.adapter.in.rest;

import eu.cleankod.carrental.domain.CarType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull CarType carType,
        @NotNull LocalDateTime start,
        @Positive int days) {
}
