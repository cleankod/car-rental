package eu.cleankod.carrental.domain;

public class InvalidRentalPeriodException extends RuntimeException {

    public InvalidRentalPeriodException(String message) {
        super(message);
    }
}
