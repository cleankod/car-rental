package eu.cleankod.carrental.adapter.in.rest;

import java.util.List;
import java.util.UUID;

public record ErrorResponse(UUID errorId, String code, String message, List<String> details) {

    static ErrorResponse of(String code, String message) {
        return new ErrorResponse(UUID.randomUUID(), code, message, List.of());
    }

    static ErrorResponse of(String code, String message, List<String> details) {
        return new ErrorResponse(UUID.randomUUID(), code, message, details);
    }
}
