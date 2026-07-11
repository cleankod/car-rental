package eu.cleankod.carrental.domain;

/**
 * No unit of the requested {@link CarType} is free for the whole requested {@link RentalPeriod}.
 */
public class CarUnavailableException extends RuntimeException {

    public CarUnavailableException(CarType carType, RentalPeriod period) {
        super("no unit of carType " + carType + " is available for period " + period);
    }
}
