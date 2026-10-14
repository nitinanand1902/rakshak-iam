package com.healthcare.dto.response;

import java.time.LocalDateTime;

/** Standard error response returned for all error conditions. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String message,
        String path,
        String errorCode
) {}
