package com.vijayshri.jobtrackerbackend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JSON body returned for every error, e.g.
 * { "timestamp": "...", "status": 404, "error": "Not Found",
 *   "message": "Job application not found with id 7" }
 * "errors" (field -> message) is only included for validation failures.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> errors
) {
}
