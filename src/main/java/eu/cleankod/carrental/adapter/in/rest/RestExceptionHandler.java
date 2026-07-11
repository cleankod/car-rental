package eu.cleankod.carrental.adapter.in.rest;

import eu.cleankod.carrental.domain.CarUnavailableException;
import eu.cleankod.carrental.domain.InvalidRentalPeriodException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Maps domain exceptions and request-parsing/validation failures to a consistent error response —
 * never the raw exception name or a stack trace.
 */
@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(CarUnavailableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponse handleCarUnavailable(CarUnavailableException exception) {
        return ErrorResponse.of("CAR_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(InvalidRentalPeriodException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleInvalidRentalPeriod(InvalidRentalPeriodException exception) {
        return ErrorResponse.of("INVALID_RENTAL_PERIOD", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleValidationFailure(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return ErrorResponse.of("VALIDATION_FAILED", "request validation failed", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleMalformedRequest(HttpMessageNotReadableException exception) {
        return ErrorResponse.of("MALFORMED_REQUEST", "request body could not be read");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ErrorResponse handleUnexpected(Exception exception) {
        return ErrorResponse.of("INTERNAL_ERROR", "an unexpected error occurred");
    }
}
