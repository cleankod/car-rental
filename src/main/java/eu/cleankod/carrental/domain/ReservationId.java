package eu.cleankod.carrental.domain;

import java.util.Objects;
import java.util.UUID;

public record ReservationId(UUID value) {

    public ReservationId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static ReservationId generate() {
        return new ReservationId(UUID.randomUUID());
    }
}
