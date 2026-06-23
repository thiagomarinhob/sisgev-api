package com.jettch.sisgev.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        List<FieldError> details
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String error, String code, String message) {
        return new ErrorResponse(Instant.now(), status, error, code, message, null);
    }

    public static ErrorResponse of(int status, String error, String code, String message, List<FieldError> details) {
        return new ErrorResponse(Instant.now(), status, error, code, message, details);
    }
}
