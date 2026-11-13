package com.ecomm.sb_ecomm.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** The one error body returned by every handler in {@link GlobalExceptionHandler}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String message,
        List<String> missingFields,
        List<InvalidField> invalidFields,
        List<String> allowedFields,
        String timestamp,
        String path) {

    public record InvalidField(String field, Object received, String problem) {}

    static ErrorResponse of(int status, String message, String path) {
        return new ErrorResponse(status, message, null, null, null, Instant.now().toString(), path);
    }

    static ErrorResponse fields(int status, String message, List<String> missing,
                                List<InvalidField> invalid, String path) {
        return new ErrorResponse(status, message, missing, invalid, null, Instant.now().toString(), path);
    }

    ErrorResponse withAllowed(List<String> allowed) {
        return new ErrorResponse(status, message, missingFields, invalidFields, allowed, timestamp, path);
    }
}
