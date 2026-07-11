package eu.cleankod.carrental.domain;

/**
 * A {@link CarTypeInventory} was constructed with a non-positive unit count.
 */
public class InvalidFleetSizeException extends RuntimeException {

    public InvalidFleetSizeException(CarType carType, int totalUnits) {
        super("totalUnits must be positive for carType " + carType + ", was: " + totalUnits);
    }
}
