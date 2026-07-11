package eu.cleankod.carrental.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A half-open time window {@code [start, start + days)}: a rental beginning at {@code start} and
 * ending exactly {@code days} full days later. Two periods that only touch at that boundary — one
 * ends the instant the other starts — do not overlap, so back-to-back reservations of the same car
 * are allowed.
 */
public record RentalPeriod(LocalDateTime start, int days) {

    public RentalPeriod {
        Objects.requireNonNull(start, "start must not be null");
        if (days <= 0) {
            throw new InvalidRentalPeriodException(start, days);
        }
    }

    public LocalDateTime endExclusive() {
        return start.plusDays(days);
    }

    public boolean overlaps(RentalPeriod other) {
        return this.start.isBefore(other.endExclusive()) && other.start.isBefore(this.endExclusive());
    }
}
