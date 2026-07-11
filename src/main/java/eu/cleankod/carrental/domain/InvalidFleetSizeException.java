package eu.cleankod.carrental.domain;

public class InvalidFleetSizeException extends RuntimeException {

    public InvalidFleetSizeException(String message) {
        super(message);
    }
}
