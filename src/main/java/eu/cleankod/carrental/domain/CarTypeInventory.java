package eu.cleankod.carrental.domain;

import java.util.Collection;
import java.util.Objects;

/**
 * The fixed number of units available for a {@link CarType}, and the rule for whether a candidate
 * reservation can be accepted given the reservations already accepted for that type. A candidate is
 * accepted only if fewer than {@code totalUnits} existing reservations overlap it — i.e. at least one
 * unit is free for the whole candidate period.
 *
 * <p>This is a conservative admission rule, not a full bin-repacking optimizer: existing reservations
 * are never reassigned between units, so in rare fragmented-inventory cases a period that a cleverer
 * reassignment could technically still fit may be rejected. Documented as a known trade-off rather
 * than an oversight — see the project README.
 */
public record CarTypeInventory(CarType carType, int totalUnits) {

    public CarTypeInventory {
        Objects.requireNonNull(carType, "carType must not be null");
        if (totalUnits <= 0) {
            throw new InvalidFleetSizeException(carType, totalUnits);
        }
    }

    public boolean hasCapacityFor(Collection<Reservation> existingReservations, Reservation candidate) {
        long overlapping = existingReservations.stream().filter(candidate::overlaps).count();
        return overlapping < totalUnits;
    }
}
