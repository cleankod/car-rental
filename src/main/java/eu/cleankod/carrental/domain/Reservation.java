package eu.cleankod.carrental.domain;

import java.util.Objects;

/**
 * A confirmed reservation of one unit of {@link CarType} for a {@link RentalPeriod}. {@link
 * ReservationId} gives it identity, distinguishing it from another reservation that happens to share
 * the same type and period.
 */
public record Reservation(ReservationId id, CarType carType, RentalPeriod period) {

    public Reservation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(carType, "carType must not be null");
        Objects.requireNonNull(period, "period must not be null");
    }

    public static Reservation of(CarType carType, RentalPeriod period) {
        return new Reservation(ReservationId.generate(), carType, period);
    }

    /** Two reservations conflict only if they are for the same car type and their periods overlap. */
    public boolean overlaps(Reservation other) {
        return this.carType == other.carType && this.period.overlaps(other.period);
    }
}
