package eu.cleankod.carrental.domain;

import java.time.LocalDateTime;

/**
 * A {@link RentalPeriod} was constructed with a non-positive duration.
 */
public class InvalidRentalPeriodException extends RuntimeException {

    public InvalidRentalPeriodException(LocalDateTime start, int days) {
        super("days must be positive for period starting at " + start + ", was: " + days);
    }
}
